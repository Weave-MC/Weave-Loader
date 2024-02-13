package net.weavemc.loader.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.weavemc.api.ModInitializer

/**
 * The data class containing information about a loaded Weave mod.
 *
 * @property modId The loaded modId of a mod, see [ModConfig's `modId`][ModConfig.modId] for more info.
 * @property config The [ModConfig][ModConfig] instance of the mod.
 */
data class WeaveMod(
    val modId: String,
    val config: ModConfig
)

/**
 * The data class that is read from a mod's `weave.mod.json`.
 *
 * @property name The loaded name of the mod, if this field is not found, the game will crash
 * @property modId The loaded mod ID of the mod, if this field is not found, the game will crash
 * @property mixinConfigs The loaded mixin configs of the mod.
 * @property hooks The loaded hooks of the mod.
 * @property entryPoints The loaded [ModInitializer] entry points of the mod.
 * @property namespace The mappings namespace this mod was created with.
 */
@Serializable
data class ModConfig(
    val name: String,
    val modId: String,
    val entryPoints: List<String> = listOf(),
    val mixinConfigs: List<String> = listOf(),
    val hooks: List<String> = listOf(),
    val namespace: String
)

@Serializable
data class MixinConfig(
    @SerialName("package")
    val packagePath: String,
    val mixins: List<String>
)
