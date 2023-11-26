package net.weavemc.loader

import net.weavemc.api.GameInfo
import net.weavemc.api.gameVersion
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.*

internal object FileManager {
    val MODS_DIRECTORY = getOrCreateDirectory("mods")
    val CACHE_DIRECTORY = getOrCreateDirectory("cache")
    val API_DIRECTORY = getOrCreateDirectory("api")

    private val apiJarNameRegex = Regex("v\\d+\\.\\d+(\\.\\d+)?\\.jar")

    fun getVanillaMinecraftJar(): File {
        val os = System.getProperty("os.name").lowercase()
        val minecraftPath = Paths.get(System.getProperty("user.home"), when {
            os.contains("win") -> "AppData${File.separator}Roaming${File.separator}.minecraft"
            os.contains("mac") -> "Library${File.separator}Application Support${File.separator}minecraft"
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> ".minecraft"
            else -> error("Failed to retrieve Vanilla Minecraft Jar due to unsupported OS.")
        })

        return minecraftPath.resolve("versions")
            .resolve(gameVersion.versionName)
            .resolve("${gameVersion.versionName}.jar").toFile()
    }

    fun getCommonApi(): File =
        API_DIRECTORY.resolve("common.jar").toFile()

    fun getVersionApi(): File? =
        API_DIRECTORY.listDirectoryEntries()
            .filter { it.name.matches(apiJarNameRegex) }
            .filter { it.isRegularFile() }
            .find { gameVersion == (GameInfo.Version.fromVersionName(it.nameWithoutExtension.removePrefix("v"))
                ?: error("Invalid API version: ${it.nameWithoutExtension}")) }
            ?.toFile()

    /**
     * Gets all mods in the `~/.weave/mods` directory.
     */
    fun getMods(): List<ModJar> {
        val mods = mutableListOf<ModJar>()

        MODS_DIRECTORY.listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .forEach { mods += ModJar.fromFile(it.toFile()) }

        val specificVersionDirectory = MODS_DIRECTORY.resolve(gameVersion.versionName)
        if (specificVersionDirectory.exists() && specificVersionDirectory.isDirectory()) {
            specificVersionDirectory.listDirectoryEntries("*.jar")
                .filter { it.isRegularFile() }
                .forEach { mods += ModJar.fromFile(it.toFile()) }
        }

        return mods
    }

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
