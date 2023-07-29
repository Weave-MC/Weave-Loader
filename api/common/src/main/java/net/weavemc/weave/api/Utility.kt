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
    val isClassExists = { className: String ->
        try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    when {
//        isClassExists("net.minecraft.client.Minecraft") -> GameInfo.Client.VANILLA
//        isClassExists("net.minecraftforge.common.MinecraftForge") -> FORGE
        isClassExists("net.labymod.core.loader.vanilla.launchwrapper.LabyModLaunchWrapperTweaker") -> LABYMOD
        isClassExists ("com.moonsworth.lunar.genesis.Genesis") -> LUNAR
//        isClassExists("net.badlion.client.Wrapper") -> GameInfo.Client.BADLION
        else -> error("Could not find game client")
    }
}

val mapper: IMapper by lazy {
    when (gameClient) {
        VANILLA -> NotchMapper(gameVersion)
        FORGE -> SeargeMapper(gameVersion)
        LABYMOD -> NotchMapper(gameVersion)
        LUNAR -> McpMapper()
        BADLION -> NotchMapper(gameVersion)
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
