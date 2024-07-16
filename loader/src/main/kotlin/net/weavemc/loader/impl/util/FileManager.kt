package net.weavemc.loader.impl.util

import me.xtrm.klog.dsl.klog
import net.weavemc.internals.GameInfo
import net.weavemc.internals.getMinecraftDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

internal object FileManager {
    private val logger by klog
    val MODS_DIRECTORY = getOrCreateDirectory("mods")
    val DUMP_DIRECTORY = getOrCreateDirectory(".bytecode.out")
    const val weaveDevModsProperty = "weave.devlauncher.mods"

    fun getVanillaMinecraftJar(): File {
        logger.trace("Searching for vanilla jar")

        run {
            val regularPath = getMinecraftDir().resolve("versions")
                .resolve(GameInfo.version.versionName)
                .resolve("${GameInfo.version.versionName}.jar")

            if (regularPath.exists()) {
                return regularPath.toFile()
            }
        }

        logger.trace("Trying to find vanilla jar in classpath")
        val gameVersion = GameInfo.version.versionName
        val mclPath = Path("versions", gameVersion, "$gameVersion.jar")
        val mmcPath = Path("libraries", "com", "mojang", "minecraft", gameVersion, "minecraft-$gameVersion-client.jar")
        val classpath = System.getProperty("java.class.path")
        val paths = classpath?.split(File.pathSeparator)?.map { Path(it) }
        return paths?.find { it.endsWith(mclPath) || it.endsWith(mmcPath) }?.toFile()
            ?: fatalError("Could not find vanilla jar for version $gameVersion")
    }

    /**
     * Gets all mods in the `~/.weave/mods` directory.
     */
    fun getMods(): List<ModJar> {
        val mods = mutableListOf<ModJar>()

        val weaveDevMods = System.getProperty(weaveDevModsProperty)
        if (weaveDevMods != null) {
            logger.info("Detected dev mod property set ($weaveDevModsProperty), skipping regular mod loading")
            weaveDevMods.splitToSequence(";").mapTo(mods) { ModJar(File(it), false) }
        } else {
            logger.trace("Searching for mods in $MODS_DIRECTORY")
            mods += MODS_DIRECTORY.walkMods()

            val specificVersionDirectory = MODS_DIRECTORY.resolve(GameInfo.version.versionName)
            if (specificVersionDirectory.exists() && specificVersionDirectory.isDirectory()) {
                logger.trace("Searching for mods in $specificVersionDirectory")
                mods += specificVersionDirectory.walkMods(true)
            }
        }

        logger.info("Discovered ${mods.size} mod files")

        return mods
    }

    private fun Path.walkMods(isSpecific: Boolean = false) = listDirectoryEntries("*.jar")
        .filter { it.isRegularFile() }
        .map { ModJar(it.toFile(), isSpecific) }

    data class ModJar(val file: File, val isSpecific: Boolean)
}
