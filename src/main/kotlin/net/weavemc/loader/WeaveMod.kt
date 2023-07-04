package net.weavemc.loader

import net.weavemc.loader.api.ModInitializer

/**
 * The data class containing information about a loaded Weave mod.
 *
 * @property instance A list of instances of [Mod Initializer][ModInitializer] entry points. Usually mods only have one of these.
 * @property name The loaded name of a mod, see [ModConfig's `name`][WeaveLoader.ModConfig.name] for more info.
 * @property config The [ModConfig][WeaveLoader.ModConfig] instance of the mod.
 */
public data class WeaveMod(
    val instance: List<ModInitializer>,
    val name: String,
    val config: WeaveLoader.ModConfig
)
