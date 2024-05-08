package net.weavemc.loader.util

import net.weavemc.internals.GameInfo
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

internal object FileManager {
    val MODS_DIRECTORY = getOrCreateDirectory("mods")
    val DUMP_DIRECTORY = getOrCreateDirectory(".bytecode.out")

    private fun buildPath(vararg parts: String) =
        parts.joinToString(File.separator)

    fun getVanillaMinecraftJar(): File {
        val os = System.getProperty("os.name").lowercase()
        run {
            val userHome = System.getProperty("user.home", System.getenv("HOME") ?: System.getenv("USERPROFILE"))
            val minecraftPath = when {
                os.contains("win") -> arrayOf("AppData", "Roaming", ".minecraft")
                os.contains("mac") -> arrayOf("Library", "Application Support", "minecraft")
                os.contains("nix") || os.contains("nux") || os.contains("aix") ->
                    arrayOf(".minecraft")
                else -> return@run
            }
            val fullPath = Path(userHome, *minecraftPath)
            val regularPath = fullPath.resolve("versions")
                .resolve(GameInfo.version.versionName)
                .resolve("${GameInfo.version.versionName}.jar")
            if (regularPath.exists()) {
                return regularPath.toFile()
            }
        }

        val gameVersion = GameInfo.version.versionName
        val mclPath = buildPath("versions", gameVersion, "$gameVersion.jar")
        val mmcPath = buildPath("libraries", "com", "mojang", "minecraft", gameVersion,
            "minecraft-$gameVersion-client.jar")
        val classpath = System.getProperty("java.class.path")
        val paths = classpath?.split(File.pathSeparator)
        return paths?.find { it.endsWith(mclPath) || it.endsWith(mmcPath) }
            ?.let { File(it) }
            ?: fatalError("Could not find vanilla jar for version $gameVersion")
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
