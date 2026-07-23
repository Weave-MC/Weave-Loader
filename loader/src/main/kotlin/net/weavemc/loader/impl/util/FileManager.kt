package net.weavemc.loader.impl.util

import me.xtrm.klog.dsl.klog
import net.weavemc.internals.GameInfo
import net.weavemc.internals.getOrCreateWeaveDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

public object FileManager {
    private val logger by klog

    public val MODS_DIRECTORY: Path by systemProperty(
        key = "weave.mods.directory",
        defaultValueProvider = { getOrCreateWeaveDir("mods") }
    )

    public val DUMP_DIRECTORY: Path by systemProperty(
        key = "weave.dump.bytecode.directory",
        defaultValueProvider = { getOrCreateWeaveDir(".bytecode.out") }
    )

    public val VANILLA_MINECRAFT_JAR: File by systemProperty(
        key = "weave.vanilla.jar.path",
        defaultValueProvider = {
            logger.trace("Searching for vanilla jar")

            val os = System.getProperty("os.name").lowercase()
            val userHome = System.getProperty("user.home", System.getenv("HOME") ?: System.getenv("USERPROFILE"))

            val minecraftPath = when {
                os.contains("win") -> Paths.get("AppData", "Roaming", ".minecraft")
                os.contains("mac") -> Paths.get("Library", "Application Support", "minecraft")
                os.contains("nix") || os.contains("nux") || os.contains("aix") -> Paths.get(".minecraft")
                else -> null
            }

            if (minecraftPath != null) {
                val fullPath = Paths.get(userHome).resolve(minecraftPath)
                val regularPath = fullPath.resolve("versions")
                    .resolve(GameInfo.version.versionName)
                    .resolve("${GameInfo.version.versionName}.jar")

                if (regularPath.exists()) {
                    logger.debug("Found vanilla jar at standard location: $regularPath")
                    return@systemProperty regularPath.toFile()
                }
            }

            logger.trace("Trying to find vanilla jar in classpath")
            val gameVersion = GameInfo.version.versionName
            val mclPath = Paths.get("versions", gameVersion, "$gameVersion.jar")
            val mmcPath = Paths.get("libraries", "com", "mojang", "minecraft", gameVersion, "minecraft-$gameVersion-client.jar")
            val classpath = System.getProperty("java.class.path")
            val paths = classpath?.split(File.pathSeparator)?.map { Paths.get(it) }

            val foundJar = paths?.find { it.endsWith(mclPath) || it.endsWith(mmcPath) }?.toFile()
            if (foundJar != null) {
                logger.debug("Found vanilla jar via classpath: ${foundJar.path}")
                return@systemProperty foundJar
            }

            logger.error("Failed to locate vanilla jar for version $gameVersion")
            fatalError("Could not find vanilla jar for version $gameVersion")
        }
    )

    public val mods: List<ModJar> by systemProperty(
        key = "weave.mods.override",
        defaultValueProvider = {
            logger.trace("Searching for mods in $MODS_DIRECTORY")
            val baseMods = MODS_DIRECTORY.walkMods(isVersionSpecific = false)
            logger.debug("Found ${baseMods.size} base mod files in $MODS_DIRECTORY")

            val specificVersionDirectory = MODS_DIRECTORY.resolve(GameInfo.version.versionName)
            val versionMods = if (specificVersionDirectory.exists() && specificVersionDirectory.isDirectory()) {
                logger.trace("Searching for version-specific mods in $specificVersionDirectory")
                specificVersionDirectory.walkMods(isVersionSpecific = true).also {
                    logger.debug("Found ${it.size} version-specific mod files in $specificVersionDirectory")
                }
            } else {
                emptyList()
            }

            val totalMods = baseMods + versionMods
            logger.info("Discovered ${totalMods.size} total mod files")
            totalMods
        },
        parser = { propertyString: String ->
            propertyString.split(File.pathSeparator)
                .map { File(it) }
                .map { ModJar(it, isVersionSpecific = true) }
        }
    )

    private fun Path.walkMods(isVersionSpecific: Boolean) = listDirectoryEntries("*.jar")
        .filter { it.isRegularFile() }
        .map { ModJar(it.toFile(), isVersionSpecific) }

    public data class ModJar(val file: File, val isVersionSpecific: Boolean)
}