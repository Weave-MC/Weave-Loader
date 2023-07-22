package net.weavemc.weave.api

/**
 * net/minecraft/client/Minecraft -> some/mapped/MCClass
 */
operator fun String.not(): String = mapUniversal(this)

/**
 * (Lnet/minecraft/client/Minecraft;)V -> (Lsome/mapped/MCClass;)V
 */
operator fun String.unaryMinus(): String = mapUniversalDesc(this)
