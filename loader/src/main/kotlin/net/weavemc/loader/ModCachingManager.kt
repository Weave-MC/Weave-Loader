package net.weavemc.loader

import net.weavemc.weave.api.*
import net.weavemc.loader.mapping.LambdaAwareRemapper
import net.weavemc.loader.mapping.MappingsRemapper
import net.weavemc.loader.mapping.environmentMapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.io.path.*

internal object ModCachingManager {
    val modsDirectory = getOrCreateDirectory("mods")
    val cacheDirectory = getOrCreateDirectory(".cache")

    /**
     * Gets the cached api and mod jars.
     *
     * @return The cached api, mapped mods, and original mods.
     */
    fun getCachedApiAndMods(): Triple<File?, List<File>, List<File>> {
        val apiJar = WeaveApiManager.getApiJar()
        val modFiles = getModFiles()
        val cacheFiles = getCacheFiles()

        if (apiJar == null) {
            // Weave events not supported for this version
            println("[Weave] Built-in events not supported for this version!")

            for (cacheFile in cacheFiles) {
                if (modFiles.any { it sha256Equals cacheFile }) {
                    continue
                }

                println("[Weave] Deleting unused cache for ${cacheFile.file.name}")
                cacheFile.file.deleteRecursively()
            }

            val mappedMods = modFiles.map {
                cacheFiles.find { cacheMod -> it sha256Equals cacheMod } ?: createCache(environmentMapper, it)
            }

            return Triple(null, mappedMods.map { it.file }, modFiles.map { it.file })
        }

        val cacheApi = ModJar.fromFile(apiJar)
        for (cacheFile in cacheFiles) {
            if (cacheApi sha256Equals cacheFile || modFiles.any { it sha256Equals cacheFile }) {
                continue
            }

            println("[Weave] Deleting unused cache for ${cacheFile.file.name}")
            cacheFile.file.deleteRecursively()
        }

        val mappedApi = cacheFiles.find { it sha256Equals cacheApi } ?: createCache(environmentMapper, cacheApi)
        val mappedMods = modFiles.map {
            cacheFiles.find { cacheMod -> it sha256Equals cacheMod } ?: createCache(environmentMapper, it)
        }

        return Triple(mappedApi.file, mappedMods.map { it.file }, modFiles.map { it.file })
    }

    /**
     * Creates a remapped cache for the given [modJar] using the given [remapper].
     *
     * @param remapper The remapper to use.
     * @param modJar The mod to remap.
     * @param outFile The file to output to.
     * @return The remapped [ModJar].
     */
    private fun createCache(
        remapper: MappingsRemapper,
        modJar: ModJar,
        outFile: Path = cacheDirectory.resolve(getCacheFileName(file = modJar.file))
    ): ModJar {
        println("[Weave] Creating cache for ${modJar.file.name} using ${remapper.mappingsType}")

        outFile.deleteIfExists()

        JarFile(modJar.file).use { jarIn ->
            JarOutputStream(outFile.outputStream()).use { jarOut ->
                val modifiedEntries = mutableListOf<Pair<JarEntry, ByteArray>>()

                jarIn.entries()
                    .asSequence()
                    .forEach { entry ->
                        val name = entry.name
                        val bytes = jarIn.getInputStream(entry).readBytes()

                        if (name.endsWith(".class")) {
                            // Process class entries and store the modified entry in the list
                            val classReader = ClassReader(bytes)
                            val classWriter = ClassWriter(classReader, 0)
                            classReader.accept(LambdaAwareRemapper(classWriter, remapper), 0)
                            modifiedEntries.add(JarEntry(entry.name) to classWriter.toByteArray())
                        } else if (!entry.isDirectory) {
                            modifiedEntries.add(JarEntry(entry.name) to bytes)
                        }
                    }

                modifiedEntries.forEach { (entry, bytes) ->
                    jarOut.putNextEntry(entry)
                    jarOut.write(bytes)
                    jarOut.closeEntry()
                }
            }
        }

        return ModJar(outFile.toFile(), modJar.sha256)
    }

    /**
     * Gets all cached mods in the `~/.weave/.cache` directory.
     * If the cache is invalid or the original mod is missing, the cache is deleted.
     */
    private fun getCacheFiles(): List<ModJar> =
        cacheDirectory.listDirectoryEntries("{${gameVersion.versionName},all}-${environmentMapper.javaClass.simpleName}-*.cache")
            .filter { it.isRegularFile() || it.isSymbolicLink() }
            .mapNotNull { f ->
                val (_, sha256) = runCatching { f.fileName.toString().split("-") }.getOrElse {
                    println("[Weave] Deleting invalid cache file ${f.fileName}")
                    f.deleteExisting()
                    return@mapNotNull null
                }

                ModJar(f.toFile(), sha256)
            }

    /**
     * Gets all mods in the `~/.weave/mods` directory.
     */
    private fun getModFiles(): List<ModJar> {
        val mods = mutableListOf<ModJar>()

        modsDirectory.listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .forEach { mods += ModJar.fromFile(it.toFile()) }

        val specificVersionDirectory = modsDirectory.resolve(gameVersion.versionName)
        if (specificVersionDirectory.exists() && specificVersionDirectory.isDirectory()) {
            specificVersionDirectory.listDirectoryEntries("*.jar")
                .filter { it.isRegularFile() }
                .forEach { mods += ModJar.fromFile(it.toFile()) }
        }

        return mods
    }

    /**
     * Gets the cache file name for the given [file].
     * The cache file name is in the format of `<gameVersion>-<mapper>-<sha256>.cache`.
     *
     * Example: `1.7-McpMapper-8f498d2e11f3e9eb016a5a1c35885b87b561f5fd1941864b2db704878bc0c79d.cache`
     */
    private fun getCacheFileName(version: GameInfo.Version = gameVersion, file: File): String =
        "${version.versionName}-${environmentMapper.mappingsType}-${file.toSha256()}.cache"

    /**
     * Represents an original mod jar or a cache file.
     *
     * @property file Either the original mod jar OR the cache file.
     * @property sha256 Either the sha256 of the mod jar OR the sha256 in the cache file name.
     * @property version Either the current game version OR the version in the cache file name.
     */
    data class ModJar(
        val file: File,
        val sha256: String,
        val version: GameInfo.Version? = gameVersion,
    ) {
        infix fun sha256Equals(other: ModJar): Boolean = sha256 == other.sha256

        companion object {
            /**
             * Creates a [ModJar] from the given [original mod jar][file].
             */
            fun fromFile(file: File): ModJar {
                val sha256 = file.toSha256()
                val version = runCatching { GameInfo.Version.fromVersionName(file.nameWithoutExtension) }.getOrNull()
                return ModJar(file, sha256, version)
            }
        }
    }
}
