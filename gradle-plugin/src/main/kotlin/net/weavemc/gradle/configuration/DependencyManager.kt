package net.weavemc.gradle.configuration

import com.grappenmaker.mappings.*
import kotlinx.serialization.Serializable
import net.weavemc.gradle.*
import net.weavemc.internals.DownloadUtil
import net.weavemc.internals.MinecraftVersion
import net.weavemc.internals.VersionInfo
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.get
import org.objectweb.asm.ClassVisitor
import java.io.File
import java.net.URL

/**
 * Pulls dependencies from [addMinecraftAssets] and [addMappedMinecraft]
 */
fun Project.pullDeps(version: MinecraftVersion, versionInfo: VersionInfo, namespace: String) {
    addMinecraftAssets(version, versionInfo)
    addMappedMinecraft(version, namespace)
}

/**
 * Adds Minecraft as a dependency by providing the jar to the projects file tree.
 */
private fun Project.addMinecraftAssets(version: MinecraftVersion, versionInfo: VersionInfo) {
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
    val ext = extensions["weave"] as WeaveMinecraftExtension
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