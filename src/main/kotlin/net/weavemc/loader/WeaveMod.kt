package net.weavemc.loader

import net.weavemc.loader.api.ModInitializer

public data class WeaveMod(
    val instance: List<ModInitializer>,
    val name: String,
    val config: WeaveLoader.ModConfig
)
