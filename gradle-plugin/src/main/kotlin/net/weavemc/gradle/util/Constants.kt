package net.weavemc.gradle.util

import kotlinx.serialization.json.Json
import net.weavemc.internals.MinecraftVersion
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.io.File

/**
 * A class containing constant mnemonic values to
 * be referenced throughout the project.
 */
public object Constants {
    /**
     * The name of the Gradle extension
     */
    public const val WEAVE_EXTENSION: String = "weave"

    /**
     * The Gradle cache directory.
     *
     *  *  Windows: `"%USERPROFILE%\.gradle\caches\weave\"`
     *  *  Linux:   `"${HOME}/.gradle/caches/weave/"`
     *  *  Mac:     `"${HOME}/.gradle/caches/weave/"`
     */
    public val CACHE_DIR: File = File(System.getProperty("user.home"), ".gradle/caches/weave")

    /**
     * The global JSON serializer
     */
    public val JSON: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * The version manifest URL
     */
    public const val VERSION_MANIFEST: String = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
}

public val MinecraftVersion.cacheDirectory: File get() = Constants.CACHE_DIR.resolve("cache-${versionName}").also { it.mkdirs() }
public val MinecraftVersion.minecraftJarCache: File get() = cacheDirectory.resolve("client.jar")

public fun Project.mappedJarCache(namespace: String, version: MinecraftVersion): File =
    localGradleCache().file("${version.minecraftJarCache.nameWithoutExtension}-$namespace.jar").asFile

public fun Project.localGradleCache(): Directory = layout.projectDirectory.dir(".gradle").dir("weave")
public fun Project.localCache(): Provider<Directory> = layout.buildDirectory.dir("weave")
