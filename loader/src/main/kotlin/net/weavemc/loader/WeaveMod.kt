package net.weavemc.loader

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.loader.mapping.MappingsHandler

/**
 * The data class containing information about a loaded Weave mod.
 *
 * @property name The loaded name of a mod, see [ModConfig's `name`][ModConfig.name] for more info.
 * @property config The [ModConfig][ModConfig] instance of the mod.
 */
data class WeaveMod(
    val name: String,
    val config: ModConfig
)

/**
 * The data class that is read from a mod's `weave.mod.json`.
 *
 * @property mixinConfigs The loaded mixin configs of the mod.
 * @property hooks The loaded hooks of the mod.
 * @property entrypoints The loaded [ModInitializer] entry points of the mod.
 * @property name The loaded name of the mod, if this field is not found, it will default to the mod's jar file.
 * @property modId The loaded mod ID of the mod, if this field is not found, it will be assigned
 *           a random placeholder value upon loading. **This value is not persistent between launches!**
 */
@Serializable
data class ModConfig(
    val mixinConfigs: List<String> = listOf(),
    val hooks: List<String> = listOf(),
    val entrypoints: List<String> = listOf(),
    val name: String? = null,
    val modId: String? = null
)

/**
 * @param hook Hook class
 */
data class ModHook(
    val hook: Hook,
)

@Serializable
public data class MixinConfig(
    @SerialName("package")
    val packagePath: String,
    val mixins: List<String>
)
