package net.weavemc.loader

import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.gameVersion
import net.weavemc.weave.api.mapper
import net.weavemc.weave.api.mapping.client.RemapperWrapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import java.io.File
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

internal object ModCachingManager {
    private val modsDirectory = getOrCreateDirectory("mods")

    private val cacheDirectory = getOrCreateDirectory(".cache")

    fun getCachedApiAndMods(): Triple<JarFile, List<JarFile>, List<JarFile>> {
        clearUnusedResources()

        val apiJar = WeaveApiManager.getApiJar()
        val cacheApi = CacheMod.fromFile(apiJar)
        val modFiles = getModFiles()

        val cacheFiles = getCacheFiles()

        for (cacheMod in cacheFiles) {
            if (cacheApi sha256Equals cacheMod || modFiles.any { it sha256Equals cacheMod }) {
                continue
            }

            println("[Weave] Deleting unused cache for ${cacheMod.file.name}")
            cacheMod.file.deleteRecursively()
        }

        val remapperWrapper by lazy { RemapperWrapper(mapper) }

        val mappedApi = cacheFiles.find { it sha256Equals cacheApi } ?: createCache(remapperWrapper, cacheApi)
        val mappedMods = modFiles.map { cacheFiles.find { cacheMod -> it sha256Equals cacheMod } ?: createCache(remapperWrapper, it) }

        return Triple(JarFile(mappedApi.file), mappedMods.map { JarFile(it.file) }, modFiles.map { JarFile(it.file) })
    }

    private fun createCache(remapper: RemapperWrapper, cacheMod: CacheMod, outFile: Path = cacheDirectory.resolve(getCacheFileName(file = cacheMod.file))): CacheMod {
        println("[Weave] Creating cache for ${cacheMod.file.name}")

        outFile.deleteIfExists()

        val jarIn = JarFile(cacheMod.file)
        val jarOut = ZipOutputStream(outFile.outputStream())

        jarIn.entries()
            .asSequence()
            .forEach {
                val name = it.name

                if (name == "weave.mod.json") {
                    jarOut.putNextEntry(it)
                    jarOut.write(jarIn.getInputStream(it).readBytes())
                    jarOut.closeEntry()
                } else if (name.endsWith(".class")) {
                    val classBytes = jarIn.getInputStream(it).readBytes()
                    val classReader = ClassReader(classBytes)
                    val classWriter = ClassWriter(classReader, 0)
                    classReader.accept(ClassRemapper(classWriter, remapper), 0)
                    val bytes = classWriter.toByteArray()
                    jarOut.putNextEntry(it)
                    jarOut.write(bytes)
                    jarOut.closeEntry()
                }
            }

        return CacheMod(outFile.toFile(), cacheMod.sha256)
    }

    private fun clearUnusedResources() {
        val cacheMods = cacheDirectory.listDirectoryEntries("{${gameVersion.versionName},all}-${mapper.javaClass.simpleName}-*.cache")
                .filter { it.isRegularFile() || it.isSymbolicLink() }

        val cacheResources = cacheDirectory.listDirectoryEntries("{${gameVersion.versionName},all}-${mapper.javaClass.simpleName}-*.cache.resource")
            .filter { it.isRegularFile() || it.isSymbolicLink() }

        for (cacheResource in cacheResources) {
            val cacheMod = cacheMods.find { it.fileName.toString().startsWith(cacheResource.fileName.toString().substringBeforeLast(".")) }
            if (cacheMod == null) {
                println("[Weave] Deleting unused cache resource ${cacheResource.fileName}")
                cacheResource.deleteExisting()
            }
        }
    }

    private fun getCacheFiles(): List<CacheMod> =
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
                CacheMod(file, sha256)
            }
            .filterNotNull()

    private fun getModFiles(): List<CacheMod> {
        val mods = mutableListOf<CacheMod>()

        modsDirectory.listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .forEach { mods += CacheMod.fromFile(it.toFile()) }

        val specificVersionDirectory = modsDirectory.resolve(gameVersion.versionName)
        if (specificVersionDirectory.exists() && specificVersionDirectory.isDirectory()) {
            specificVersionDirectory.listDirectoryEntries("*.jar")
                .filter { it.isRegularFile() }
                .forEach { mods += CacheMod.fromFile(it.toFile()) }
        }

        return mods
    }

    private fun getCacheFileName(version: GameInfo.Version = gameVersion, file: File): String = "${version.versionName}-${mapper.javaClass.simpleName}-${file.toSha256()}.cache"

    data class CacheMod(
        val file: File,
        val sha256: String,
        val version: GameInfo.Version? = gameVersion,
    ) {
        infix fun sha256Equals(other: CacheMod): Boolean = sha256 == other.sha256

        companion object {
            fun fromFile(file: File): CacheMod {
                val sha256 = file.toSha256()
                val version = runCatching { GameInfo.Version.fromVersionName(file.nameWithoutExtension) }.getOrNull()
                return CacheMod(file, sha256)
            }
        }
    }
}
