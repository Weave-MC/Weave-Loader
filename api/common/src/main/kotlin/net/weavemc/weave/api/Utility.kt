@file:JvmName("Utility")

package net.weavemc.weave.api

import net.weavemc.weave.api.GameInfo.Client.*
import net.weavemc.weave.api.mapping.*

val gameInfo by lazy {
    GameInfo(
        gameVersion,
        gameClient
    )
}

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
    val isLunar = command.contains("Genesis")
    val version =  """--version\s+(\S+)""".toRegex().find(System.getProperty("sun.java.command"))?.groupValues?.get(1)?.lowercase() ?: error("Failed to retrieve version from command line")

    when {
        isLunar -> LUNAR
        version.contains("forge") -> FORGE
        version.contains("labymod") -> LABYMOD
        else -> error("Could not find game client")
    }
}

val mapper: IMapper by lazy {
    when (gameClient) {
        VANILLA -> XSrgRemapper(gameVersion, "notch")
        FORGE -> XSrgRemapper(gameVersion, "searge")
        LABYMOD -> XSrgRemapper(gameVersion, "notch")
        LUNAR -> GenericRemapper()
        BADLION -> XSrgRemapper(gameVersion, "notch")
    }
}

fun getMappedMethod(
    owner: String,
    name: String,
    descriptor: String
): MappedMethod? = mapper.mapMethod(owner, name, descriptor)

fun getMappedField(
    owner: String,
    name: String
): MappedField? = mapper.mapField(owner, name)

fun getMappedClass(
    name: String
): String? = mapper.mapClass(name)
