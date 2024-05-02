package net.weavemc.loader.util

import net.weavemc.internals.GameInfo
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

internal object FileManager {
    val MODS_DIRECTORY = getOrCreateDirectory("mods")
    val DUMP_DIRECTORY = getOrCreateDirectory(".bytecode.out")

    fun getVanillaMinecraftJar(): File {
        val os = System.getProperty("os.name").lowercase()
        val minecraftPath = when {
            os.contains("win") -> "AppData${File.separator}Roaming${File.separator}.minecraft"
            os.contains("mac") -> "Library${File.separator}Application Support${File.separator}minecraft"
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> ".minecraft"
            else -> null
        }
        if (minecraftPath != null) {
            val fullPath = Paths.get(System.getProperty("user.home"), minecraftPath)

            val regularPath = fullPath.resolve("versions")
                .resolve(GameInfo.version.versionName)
                .resolve("${GameInfo.version.versionName}.jar")
            if (regularPath.exists()) {
                return regularPath.toFile()
            }
        }

        val gameVersion = GameInfo.version.versionName
        val classpath = System.getProperty("java.class.path")
        val separator = File.pathSeparator
        val paths = classpath.split(separator)
        for (path in paths) {
            // .minecraft/versions/<ver>/<ver>.jar
            if (path.endsWith("/$gameVersion/$gameVersion.jar")) {
                return File(path)
            }
            // MultiMC-like maven structure
            if (path.endsWith("com/mojang/minecraft/$gameVersion/minecraft-$gameVersion-client.jar")) {
                return File(path)
            }
        }
        fatalError("Could not find vanilla jar for version $gameVersion")
    }

    /**
     * Gets all mods in the `~/.weave/mods` directory.
     */
    fun getMods(): List<ModJar> {
        val mods = mutableListOf<ModJar>()

        mods += MODS_DIRECTORY.walkMods()

        val specificVersionDirectory = MODS_DIRECTORY.resolve(GameInfo.version.versionName)
        if (specificVersionDirectory.exists() && specificVersionDirectory.isDirectory()) {
            mods += specificVersionDirectory.walkMods(true)
        }

        println("Discovered ${mods.size} mod files")

        return mods
    }

    private fun Path.walkMods(isSpecific: Boolean = false) = listDirectoryEntries("*.jar")
        .filter { it.isRegularFile() }
        .map { ModJar(it.toFile(), isSpecific) }

    data class ModJar(val file: File, val isSpecific: Boolean)
}
