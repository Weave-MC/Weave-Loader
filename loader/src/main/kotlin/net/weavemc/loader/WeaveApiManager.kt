package net.weavemc.loader

import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.gameVersion
import java.io.File
import java.util.jar.JarFile
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

object WeaveApiManager {
    private val apiJarNameRegex = Regex("v\\d+\\.\\d+(\\.\\d+)?\\.jar")

    private val apiDirectory = getOrCreateDirectory("api")

    fun getCommonApiJar(): File =
        apiDirectory.resolve("common.jar").toFile()

    fun getApiJar(): File =
        apiDirectory.listDirectoryEntries()
            .filter { it.name.matches(apiJarNameRegex) }
            .filter { it.isRegularFile() }
            .find { gameVersion == (GameInfo.Version.fromVersionName(it.nameWithoutExtension.removePrefix("v")) ?: error("Invalid API version: ${it.nameWithoutExtension}")) }
            ?.toFile()
            ?: error("No API jar found for version ${gameVersion.versionName}")
}
