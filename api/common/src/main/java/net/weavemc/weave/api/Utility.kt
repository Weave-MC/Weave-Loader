@file:JvmName("Utility")

package net.weavemc.weave.api

import net.weavemc.weave.api.GameInfo.Client.*
import net.weavemc.weave.api.mapping.IMapper
import net.weavemc.weave.api.mapping.McpMapper
import net.weavemc.weave.api.mapping.NotchMapper

val gameInfo by lazy {
    GameInfo(
        gameVersion ?: error("Could not find game version"),
        gameClient ?: error("Could not find game client")
    )
}

val gameVersion: GameInfo.Version by lazy {
    """--version\s+(\S+)""".toRegex()
        .find(System.getProperty("sun.java.command"))
        ?.groupValues?.get(1)
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
//        isClassExists("net.minecraftforge.common.MinecraftForge") -> GameInfo.Client.FORGE
//        isClassExists("net.labymod.main.LabyMod") -> GameInfo.Client.LABYMOD
        isClassExists("com.moonsworth.lunar.genesis.Genesis") -> LUNAR
//        isClassExists("net.badlion.client.Wrapper") -> GameInfo.Client.BADLION
        else -> error("Could not find game client")
    }
}

val mapper: IMapper by lazy {
    when (gameClient) {
        VANILLA -> NotchMapper(gameVersion)
        FORGE -> NotchMapper(gameVersion)
        LABYMOD -> NotchMapper(gameVersion)
        LUNAR -> McpMapper()
        BADLION -> NotchMapper(gameVersion)
    }
}

/**
 * net/minecraft/client/Minecraft -> some/mapped/MCClass
 */
operator fun String.not(): String = mapper.mapUniversal(this) ?: this

/**
 * (Lnet/minecraft/client/Minecraft;)V -> (Lsome/mapped/MCClass;)V
 */
operator fun String.unaryMinus(): String {
    val stringBuilder = StringBuilder()
    var index = 0
    while (index < length) {
        val char = this[index]
        if (char == 'L') {
            val endIndex = indexOf(';', index)
            stringBuilder.append('L')
            stringBuilder.append(!substring(index + 1, endIndex))
            stringBuilder.append(';')
            index = endIndex
        } else {
            stringBuilder.append(char)
        }
        index++
    }
    return stringBuilder.toString()
}
