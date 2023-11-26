@file:JvmName("Utility")

package net.weavemc.api

import net.weavemc.api.GameInfo.Client.*

val gameInfo by lazy { GameInfo(gameVersion, gameClient) }
val command: String = System.getProperty("sun.java.command") ?: error("Could not find command")
val realCommand: String
    get() = with(System.getProperties()["weave.extra.launch.params"] as? MutableMap<String, List<String>>) {
        if (this != null) {
            (this["param"] as List<String>).joinToString(" ")
        } else {
            command
        }
    }

val gameVersion: GameInfo.Version by lazy {
    val versionRegex = Regex("""--version\s+(?:\S*?)?(\d+\.\d+(?:\.\d+)?)""")

    versionRegex
        .find(realCommand)
        ?.groupValues
        ?.get(1)
        ?.let(GameInfo.Version::fromVersionName)
        ?: error("Could not find game version")
}

val gameClient: GameInfo.Client by lazy {
    val isLunar = "Genesis" in command
    val version = realCommand.lowercase()

    when {
        isLunar -> LUNAR
        "forge" in version -> FORGE
        "labymod" in version -> LABYMOD
        else -> VANILLA
    }
}

val gameLauncher: GameInfo.Launcher by lazy {
    fun classExists(name: String): Boolean = GameInfo::class.java.classLoader.getResourceAsStream("${name.replace('.', '/')}.class") != null

    when {
        classExists("org.multimc.EntryPoint") -> GameInfo.Launcher.MULTIMC
        classExists("org.prismlauncher.EntryPoint") -> GameInfo.Launcher.PRISM
        else -> GameInfo.Launcher.OTHER
    }
}