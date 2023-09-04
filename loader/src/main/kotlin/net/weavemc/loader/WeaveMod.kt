package net.weavemc.loader

/**
 * The data class containing information about a loaded Weave mod.
 *
 * @property name The loaded name of a mod, see [ModConfig's `name`][WeaveLoader.ModConfig.name] for more info.
 * @property config The [ModConfig][WeaveLoader.ModConfig] instance of the mod.
 */
data class WeaveMod(
    val name: String,
    val config: WeaveLoader.ModConfig
)
