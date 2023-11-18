package net.weavemc.loader

import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.gameVersion
import net.weavemc.weave.api.mapper
import net.weavemc.weave.api.mapping.RemapperWrapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import java.io.File
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

internal object ModCachingManager {
    private val modsDirectory = getOrCreateDirectory("mods")

    private val cacheDirectory = getOrCreateDirectory(".cache")

    /**
     * Gets the cached api and mod jars.
     *
     * @return The cached api, mapped mods, and original mods.
     */
    fun getCachedApiAndMods(): Triple<File, List<File>, List<File>> {
        val apiJar = WeaveApiManager.getApiJar()
        val cacheApi = ModJar.fromFile(apiJar)
        val modFiles = getModFiles()

        val cacheFiles = getCacheFiles()

        for (cacheFile in cacheFiles) {
            if (cacheApi sha256Equals cacheFile || modFiles.any { it sha256Equals cacheFile }) {
                continue
            }

            println("[Weave] Deleting unused cache for ${cacheFile.file.name}")
            cacheFile.file.deleteRecursively()
        }

        val remapperWrapper by lazy { RemapperWrapper(mapper) }

        val mappedApi = cacheFiles.find { it sha256Equals cacheApi } ?: createCache(remapperWrapper, cacheApi)
        val mappedMods = modFiles.map { cacheFiles.find { cacheMod -> it sha256Equals cacheMod } ?: createCache(remapperWrapper, it) }

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
    private fun createCache(remapper: RemapperWrapper, modJar: ModJar, outFile: Path = cacheDirectory.resolve(getCacheFileName(file = modJar.file))): ModJar {
        println("[Weave] Creating cache for ${modJar.file.name} using ${remapper.getMapperName()}")

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
                            classReader.accept(ClassRemapper(classWriter, remapper), 0)
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
        cacheDirectory.listDirectoryEntries("{${gameVersion.versionName},all}-${mapper.javaClass.simpleName}-*.cache")
            .filter { it.isRegularFile() || it.isSymbolicLink() }
            .map {
                val (_, sha256) = runCatching { it.fileName.toString().split("-") }
                    .getOrNull()
                    ?: run {
                        println("[Weave] Deleting invalid cache file ${it.fileName}")
                        it.deleteExisting()
                        return@map null
                    }
                val file = it.toFile()
                ModJar(file, sha256)
            }
            .filterNotNull()

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
     * Example: `1.7.10-McpMapper-8f498d2e11f3e9eb016a5a1c35885b87b561f5fd1941864b2db704878bc0c79d.cache`
     */
    private fun getCacheFileName(version: GameInfo.Version = gameVersion, file: File): String = "${version.versionName}-${mapper.getMapperName()}-${file.sha256}.cache"

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
                val sha256 = file.sha256
                val version = runCatching { GameInfo.Version.fromVersionName(file.nameWithoutExtension) }.getOrNull()
                return ModJar(file, sha256, version)
            }
        }
    }
}
