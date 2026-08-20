package net.weavemc.gradle.configuration

import net.weavemc.internals.MappingsType
import net.weavemc.internals.MinecraftVersion
import net.weavemc.internals.ModConfig
import org.gradle.api.provider.Property
import kotlin.properties.Delegates

public interface WeaveMinecraftExtension {
    public val version: Property<MinecraftVersion>
    public val configuration: Property<ModConfig>

    public fun version(versionString: String): Unit = version.set(
        MinecraftVersion.parse(versionString) ?: error("Unknown version $versionString")
    )

    public fun configure(block: ConfigurationBuilder.() -> Unit): Unit = configuration.set(buildConfiguration(block))
}

public inline fun buildConfiguration(block: ConfigurationBuilder.() -> Unit): ModConfig = ConfigurationBuilder().also(block).backing

public class ConfigurationBuilder {
    @PublishedApi
    internal var backing: ModConfig = ModConfig("Unnamed mod", "unnamed-mod", namespace = "official")

    private fun <T> mutatingProperty(initial: T, mut: ModConfig.(T) -> ModConfig) =
        Delegates.observable(initial) { _, _, n -> backing = backing.mut(n) }

    public var name: String by mutatingProperty(backing.name) { copy(name = it) }
    public var modId: String by mutatingProperty(backing.modId) { copy(modId = it) }
    public var namespace: String by mutatingProperty(backing.namespace) { copy(namespace = it) }
    public var dependencies: List<String> by mutatingProperty(backing.dependencies) { copy(dependencies = it) }
    public var entryPoints: List<String> by mutatingProperty(backing.entryPoints) { copy(entryPoints = it) }
    public var tweakers: List<String> by mutatingProperty(backing.tweakers) { copy(tweakers = it) }
    public var accessWideners: List<String> by mutatingProperty(backing.accessWideners) { copy(accessWideners = it) }
    public var hooks: List<String> by mutatingProperty(backing.hooks) { copy(hooks = it) }
    public var mixinConfigs: List<String> by mutatingProperty(backing.mixinConfigs) { copy(mixinConfigs = it) }

    public fun yarnMappings() {
        namespace = MappingsType.YARN.named
    }

    public fun mojangMappings() {
        namespace = MappingsType.MOJANG.named
    }

    public fun mcpMappings() {
        namespace = MappingsType.MCP.named
    }
}
