package net.weavemc.gradle.configuration

import com.grappenmaker.mappings.*
import kotlinx.serialization.Serializable
import net.weavemc.gradle.loadMergedMappings
import net.weavemc.gradle.sourceSets
import net.weavemc.gradle.util.*
import net.weavemc.internals.MinecraftVersion
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.get
import org.objectweb.asm.ClassVisitor
import java.io.File
import java.net.URL

private inline fun <reified T> String?.decodeJSON() =
    if (this != null) Constants.JSON.decodeFromString<T>(this) else null

/**
 * Pulls dependencies from [addMinecraftAssets] and [addMappedMinecraft]
 */
fun Project.pullDeps(version: MinecraftVersion, namespace: String) {
    addMinecraftAssets(version)
    addMappedMinecraft(version, namespace)
}

/**
 * Adds Minecraft as a dependency by providing the jar to the projects file tree.
 */
private fun Project.addMinecraftAssets(version: MinecraftVersion) {
    val manifest = DownloadUtil.fetch(Constants.VERSION_MANIFEST).decodeJSON<VersionManifest>() ?: return
    val versionEntry = manifest.versions.find { it.id == version.versionName } ?: return
    val versionInfo = DownloadUtil.fetch(versionEntry.url).decodeJSON<VersionInfo>() ?: return

    val client = versionInfo.downloads.client
    DownloadUtil.checksumAndDownload(URL(client.url), client.sha1, version.minecraftJarCache.toPath())

    repositories.maven {
        name = "mojang"
        setUrl("https://libraries.minecraft.net/")
    }

    versionInfo.libraries.filter { "twitch-platform" !in it.name && "twitch-external" !in it.name }
        .forEach { dependencies.add("compileOnly", it.name) }
}

private fun Project.retrieveWideners(): List<File> {
    // Cursed code
    val ext = extensions["minecraft"] as WeaveMinecraftExtension
    val wideners = ext.configuration.get().accessWideners.toHashSet()
    val widenerFiles = mutableListOf<File>()

    a@ for (set in project.sourceSets) {
        for (dir in set.resources.sourceDirectories) {
            wideners.removeIf { left ->
                val f = dir.resolve(left)
                f.exists().also { if (it) widenerFiles += f }
            }

            if (wideners.isEmpty()) break@a
        }
    }

    require(wideners.isEmpty()) { "Could not resolve access wideners $wideners! Double-check if the file exists" }
    return widenerFiles
}

private fun File.loadWidener() = loadAccessWidener(readText().trim().lines())

private fun Project.addMappedMinecraft(version: MinecraftVersion, namespace: String) = runCatching {
    val fullMappings = version.loadMergedMappings()
    val allWideners = retrieveWideners().map { it.loadWidener().remap(fullMappings, namespace) }
    val joinedWideners = allWideners.takeIf { it.isNotEmpty() }?.join()
    val joinedFile = localGradleCache().file("joined.accesswidener").asFile
    val mapped = mappedJarCache(namespace, version)

    if (
        // should be able to simplify this, right?
        !mapped.exists() ||
        (allWideners.isNotEmpty() xor joinedFile.exists()) ||
        (allWideners.isNotEmpty() && joinedFile.loadWidener() != joinedWideners)
    ) {
        logger.log(LogLevel.LIFECYCLE, "Remapping vanilla jar to $namespace for version ${version.versionName}")
        mapped.parentFile.mkdirs() // TODO use NIO api?

        val visitor = joinedWideners?.toTree()?.let { { parent: ClassVisitor -> AccessWidenerVisitor(parent, it) } }
        remapJar(fullMappings, version.minecraftJarCache, mapped, to = namespace, visitor = visitor)
    }

    joinedWideners?.write()?.let { joinedFile.writeText(it.joinToString("\n")) } ?: joinedFile.delete()

    dependencies.add("compileOnly", project.files(mapped))
}.onFailure { it.printStackTrace() }

@Serializable
private data class VersionManifest(val versions: List<ManifestVersion>)

@Serializable
private data class ManifestVersion(val id: String, val url: String)

@Serializable
private data class VersionInfo(val downloads: VersionDownloads, val libraries: List<Library>)

@Serializable
private data class VersionDownloads(val client: VersionDownload)

@Serializable
private data class VersionDownload(val url: String, val sha1: String)

@Serializable
private data class Library(val name: String)
