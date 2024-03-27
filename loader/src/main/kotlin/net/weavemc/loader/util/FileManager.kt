package net.weavemc.loader.util

import net.weavemc.internals.GameInfo.gameVersion
import net.weavemc.internals.MinecraftVersion
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

internal object FileManager {
    val MODS_DIRECTORY = getOrCreateDirectory("mods")
    val DUMP_DIRECTORY = getOrCreateDirectory(".bytecode.out")

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

    /**
     * Gets all mods in the `~/.weave/mods` directory.
     */
    fun getMods(): List<ModJar> {
        val mods = mutableListOf<ModJar>()

        mods += MODS_DIRECTORY.walkMods()

        val specificVersionDirectory = MODS_DIRECTORY.resolve(gameVersion.versionName)
        if (specificVersionDirectory.exists() && specificVersionDirectory.isDirectory()) {
            mods += specificVersionDirectory.walkMods(true)
        }

        return mods
    }

    private fun Path.walkMods(isSpecific: Boolean = false) = listDirectoryEntries("*.jar")
        .filter { it.isRegularFile() }
        .map { ModJar(it.toFile(), isSpecific) }

    data class ModJar(val file: File, val isSpecific: Boolean)
}
