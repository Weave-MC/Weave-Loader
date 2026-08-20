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
public data class ModConfig(
    public val name: String,
    public val modId: String,
    public val entryPoints: List<String> = emptyList(),
    public val mixinConfigs: List<String> = emptyList(),
    public val hooks: List<String> = emptyList(),
    public val tweakers: List<String> = emptyList(),
    public val accessWideners: List<String> = emptyList(),
    public val namespace: String,
    public val dependencies: List<String> = emptyList(),
    public val compiledFor: String? = null,
)