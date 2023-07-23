@file:JvmName("Utility")

package net.weavemc.weave.api

import net.weavemc.weave.api.GameInfo.Client.*
import net.weavemc.weave.api.mapping.IMapper
import net.weavemc.weave.api.mapping.McpMapper
import net.weavemc.weave.api.mapping.NotchMapper
import net.weavemc.weave.api.mapping.SeargeMapper

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
        .also { it?.forEach(::println) }
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
        isClassExists("net.minecraftforge.common.MinecraftForge") -> GameInfo.Client.FORGE
//        isClassExists("net.labymod.core.loader.vanilla.launchwrapper.LabyModLaunchWrapperTweaker") -> LABYMOD
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

fun mapUniversal(name: String): String = mapper.mapUniversal(name) ?: name
fun mapUniversalDesc(desc: String): String {
    val stringBuilder = StringBuilder()
    var index = 0
    while (index < desc.length) {
        val char = desc[index]
        if (char == 'L') {
            val endIndex = desc.indexOf(';', index)
            stringBuilder.append('L')
            stringBuilder.append(mapUniversal(desc.substring(index + 1, endIndex)))
            stringBuilder.append(';')
            index = endIndex
        } else {
            stringBuilder.append(char)
        }
        index++
    }
    return stringBuilder.toString()
}
