package net.weavemc.gradle

import kotlinx.serialization.json.Json
import net.weavemc.internals.MinecraftVersion
import org.gradle.api.Project
import java.io.File

/**
 * A class containing constant mnemonic values to
 * be referenced throughout the project.
 */
object Constants {
    /**
     * The gradle cache directory.
     *
     *  *  Windows: `"%USERPROFILE%\.gradle\caches\weave\"`
     *  *  Linux:   `"${HOME}/.gradle/caches/weave/"`
     *  *  Mac:     `"${HOME}/.gradle/caches/weave/"`
     */
    val CACHE_DIR = File(System.getProperty("user.home"), ".gradle/caches/weave")

    /**
     * The global JSON serializer
     */
    val JSON = Json { ignoreUnknownKeys = true }
}

val MinecraftVersion.cacheDirectory get() = Constants.CACHE_DIR.resolve("cache-${versionName}").also { it.mkdirs() }
val MinecraftVersion.minecraftJarCache get() = cacheDirectory.resolve("client.jar")

fun Project.mappedJarCache(namespace: String, version: MinecraftVersion) =
    localGradleCache().file("${version.minecraftJarCache.nameWithoutExtension}-$namespace.jar").asFile

fun Project.localGradleCache() = layout.projectDirectory.dir(".gradle").dir("weave")
fun Project.localCache() = layout.buildDirectory.dir("weave")
