package net.weavemc.internals

import kotlin.enums.enumEntries
import kotlin.properties.ReadOnlyProperty

public enum class MinecraftVersion(
    public val protocol: Int,
    public val versionName: String,
    public val majorVersion: String,
    public val mappingName: String,
    public vararg val aliases: String,
) {
    V1_7_10(5, "1.7.10", "1.7", "1.7.10", "1.7"),
    V1_8_9(47, "1.8.9", "1.8", "1.8.9", "1.8"),
    V1_12_2(340, "1.12.2", "1.12", "1.12.2", "1.12"),
    V1_16_5(754, "1.16.5", "1.16", "1.16.5", "1.16"),
    V1_20_6(766, "1.20.6", "1.20", "1.20.6","1.20"),
    V1_21_11(774, "1.21.11", "1.21", "1.21.11", "1.21");

    public companion object {
        public fun fromProtocol(protocol: Int): MinecraftVersion? = entries.find { it.protocol == protocol }

        public fun fromVersionName(versionName: String): MinecraftVersion? = entries.find { versionName.contains(it.versionName) }

        public fun fromAlias(alias: String): MinecraftVersion? = entries.find { it.aliases.contains(alias) }

        public fun parse(string: String): MinecraftVersion? = fromVersionName(string)
            ?: fromAlias(string)
            ?: string.splitToMajor().let { (p1, p2) ->
                entries.find { entry ->
                    entry
                        .aliases
                        .map { it.splitToMajor() }
                        .any { (e1, e2) -> p1 == e1 && p2 == e2 }
                }
            }

        private fun String.splitToMajor(): Pair<String, String> = split('.').take(2).let { (p1, p2) -> p1 to p2 }
    }
}

public enum class MinecraftClient(
    public val clientName: String,
    public vararg val aliases: String,
) {
    VANILLA("Vanilla"),
    FORGE("Forge", "MinecraftForge", "Minecraft Forge"),
    FABRIC("Fabric"),
    LABYMOD("LabyMod", "Laby"),
    LUNAR("Lunar Client", "Lunar", "LunarClient"),
    BADLION("Badlion Client", "BLC", "Badlion", "BadlionClient");

    public companion object {
        public fun fromClientName(clientName: String): MinecraftClient? =
            entries.find { it.clientName.equals(clientName, ignoreCase = true) }
                ?: entries.find { it.aliases.any { alias -> alias.equals(clientName, ignoreCase = true) } }
    }
}

public object GameInfo {
    @Suppress("UNCHECKED_CAST")
    public val rawGameInfo: Map<String, String>
        get() = System.getProperties()["weave.game.info"] as? Map<String, String>
            ?: error("Failed to retrieve Minecraft arguments")

    public val commandLineArgs: String = System.getProperty("sun.java.command")
        ?: error("Failed to retrieve command line arguments, this should never happen.")

    public val versionString: String by lazy {
        rawGameInfo["version"]?.lowercase() ?: error("Could not parse version from arguments")
    }

    public val version: MinecraftVersion by lazy {
        versionString.let(MinecraftVersion::parse) ?: error("Could not find game version")
    }

    public val clientString: String by lazy {
        rawGameInfo["client"]?.lowercase() ?: error("Could not parse client from arguments")
    }

    public val client: MinecraftClient by lazy {
        clientString.let(MinecraftClient::fromClientName) ?: error("Could not find game client")
    }
}

public enum class MappingsType(public val id: String) {
    MOJANG("mojmap"),
    MCP("mcp"),
    YARN("yarn"),
    MERGED("merged");

    public companion object {
        /**
         * Converts the mapping String into a [MappingsType] enum.
         *
         * @param mappings The mappings String.
         * @return The [MappingsType] corresponding [id].
         * @throws IllegalArgumentException If there is no mappings which corresponds with the [id].
         */
        @JvmStatic
        public fun fromString(mappings: String): MappingsType =
            enumEntries<MappingsType>().find { it.id == mappings } ?: error("No such mappings: $mappings")
    }

    private fun resolvingProperty() = ReadOnlyProperty<MappingsType, _> { _, prop -> resolve(prop.name) }
    public val named: String by resolvingProperty()
    public val srg: String by resolvingProperty()
    public val obf: String by resolvingProperty()
    public val intermediary: String by resolvingProperty()

    public fun resolve(type: String): String = "$id-$type"
}