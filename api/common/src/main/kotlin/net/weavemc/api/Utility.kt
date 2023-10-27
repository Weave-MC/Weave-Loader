@file:JvmName("Utility")

package net.weavemc.api

import net.weavemc.api.GameInfo.Client.*

val gameInfo by lazy { GameInfo(gameVersion, gameClient) }
val command: String = System.getProperty("sun.java.command") ?: error("Could not find command")

val gameVersion: GameInfo.Version by lazy {
    """--version\s+(?:\S*?)?(\d+\.\d+(?:\.\d+)?)"""
        .toRegex()
        .find(command)?.groupValues
        ?.get(1)
        ?.let(GameInfo.Version::fromVersionName)
        ?: error("Could not find game version")
}

val gameClient: GameInfo.Client by lazy {
    val isLunar = "Genesis" in command
    val version = """--version\s+(\S+)""".toRegex().find(System.getProperty("sun.java.command"))
        ?.groupValues?.get(1)?.lowercase() ?: error("Failed to retrieve version from command line")

    when {
        isLunar -> LUNAR
        "forge" in version -> FORGE
        "labymod" in version -> LABYMOD
        else -> VANILLA
    }
}