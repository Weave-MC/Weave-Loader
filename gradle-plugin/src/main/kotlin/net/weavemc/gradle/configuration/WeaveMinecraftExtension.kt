package net.weavemc.gradle.configuration

import net.weavemc.internals.MappingsType
import net.weavemc.internals.MinecraftVersion
import net.weavemc.internals.ModConfig
import org.gradle.api.provider.Property
import kotlin.properties.Delegates

interface WeaveMinecraftExtension {
    val version: Property<MinecraftVersion>
    val configuration: Property<ModConfig>

    fun version(versionString: String) = version.set(
        MinecraftVersion.fromVersionName(versionString) ?: error("Unknown version $versionString")
    )

    fun configure(block: ConfigurationBuilder.() -> Unit) = configuration.set(buildConfiguration(block))
}

inline fun buildConfiguration(block: ConfigurationBuilder.() -> Unit) = ConfigurationBuilder().also(block).backing

class ConfigurationBuilder {
    @PublishedApi
    internal var backing = ModConfig("Unnamed mod", "unnamed-mod", namespace = "official")

    private fun <T> mutatingProperty(initial: T, mut: ModConfig.(T) -> ModConfig) =
        Delegates.observable(initial) { _, _, n -> backing = backing.mut(n) }

    var name by mutatingProperty(backing.name) { copy(name = it) }
    var modId by mutatingProperty(backing.modId) { copy(modId = it) }
    var namespace by mutatingProperty(backing.namespace) { copy(namespace = it) }
    var dependencies by mutatingProperty(backing.dependencies) { copy(dependencies = it) }
    var entryPoints by mutatingProperty(backing.entryPoints) { copy(entryPoints = it) }
    var tweakers by mutatingProperty(backing.tweakers) { copy(tweakers = it) }
    var accessWideners by mutatingProperty(backing.accessWideners) { copy(accessWideners = it) }
    var hooks by mutatingProperty(backing.hooks) { copy(hooks = it) }
    var mixinConfigs by mutatingProperty(backing.mixinConfigs) { copy(mixinConfigs = it) }

    fun yarnMappings() {
        namespace = MappingsType.YARN.named
    }

    fun mojangMappings() {
        namespace = MappingsType.MOJANG.named
    }

    fun mcpMappings() {
        namespace = MappingsType.MCP.named
    }
}
