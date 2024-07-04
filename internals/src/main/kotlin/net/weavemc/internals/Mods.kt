package net.weavemc.internals

import kotlinx.serialization.Serializable

/**
 * The data class that is read from a mod's `weave.mod.json`.
 *
 * @property name The loaded name of the mod, if this field is not found, the game will crash
 * @property modId The loaded mod ID of the mod, if this field is not found, the game will crash
 * @property mixinConfigs The loaded mixin configs of the mod.
 * @property hooks The loaded hooks of the mod.
 * @property tweakers Names of classes that can be loaded as Tweakers
 * @property accessWideners Paths to configurations embedded within mods for access widening
 * @property entryPoints The loaded [ModInitializer] entry points of the mod.
 * @property namespace The mappings namespace this mod was created with.
 * @property dependencies The dependencies this mod requires, at a list of [modId]s.
 * @property compiledFor The version id this mod was compiled for
 */
@Serializable
data class ModConfig(
    val name: String,
    val modId: String,
    val entryPoints: List<String> = emptyList(),
    val mixinConfigs: List<String> = emptyList(),
    val hooks: List<String> = emptyList(),
    val tweakers: List<String> = emptyList(),
    val accessWideners: List<String> = emptyList(),
    val namespace: String,
    val dependencies: List<String> = emptyList(),
    val compiledFor: String? = null,
)