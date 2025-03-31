package net.weavemc.internals

import kotlin.properties.ReadOnlyProperty

enum class MinecraftVersion(
    val protocol: Int,
    val versionName: String,
    val mappingName: String,
    vararg val aliases: String,
) {
    V1_7_10(5, "1.7.10", "1.7", "1.7"),
    V1_8_9(47, "1.8.9", "1.8", "1.8"),
    V1_12_2(340, "1.12.2", "1.12", "1.12"),
    V1_16_5(754, "1.16.5", "1.16", "1.16"),
    V1_20_1(763, "1.20.1", "1.20","1.20");

    companion object {
        fun fromProtocol(protocol: Int): MinecraftVersion? = entries.find { it.protocol == protocol }
        fun fromVersionName(versionName: String): MinecraftVersion? =
            entries.find { versionName.contains(it.versionName) }
                ?: entries.find { it.aliases.any { alias -> versionName.contains(alias) } }

        fun fromAlias(alias: String): MinecraftVersion? = entries.find { it.aliases.contains(alias) }
    }
}

enum class MinecraftClient(
    val clientName: String,
    vararg val aliases: String,
) {
    VANILLA("Vanilla"),
    FORGE("Forge", "MinecraftForge", "Minecraft Forge"),
    LABYMOD("LabyMod", "Laby"),
    LUNAR("Lunar Client", "Lunar", "LunarClient"),
    BADLION("Badlion Client", "BLC", "Badlion", "BadlionClient");

    companion object {
        fun fromClientName(clientName: String): MinecraftClient? =
            entries.find { it.clientName.equals(clientName, ignoreCase = true) }
                ?: entries.find { it.aliases.any { alias -> alias.equals(clientName, ignoreCase = true) } }
    }
}

object GameInfo {
    @Suppress("UNCHECKED_CAST")
    val rawGameInfo: Map<String, String>
        get() = System.getProperties()["weave.game.info"] as? Map<String, String>
            ?: error("Failed to retrieve Minecraft arguments")

    val commandLineArgs = System.getProperty("sun.java.command")
        ?: error("Failed to retrieve command line arguments, this should never happen.")

    val versionString: String by lazy {
        rawGameInfo["version"]?.lowercase() ?: error("Could not parse version from arguments")
    }

    val version: MinecraftVersion by lazy {
        versionString.let(MinecraftVersion::fromVersionName) ?: error("Could not find game version")
    }

    val clientString: String by lazy {
        rawGameInfo["client"]?.lowercase() ?: error("Could not parse client from arguments")
    }

    val client: MinecraftClient by lazy {
        clientString.let(MinecraftClient::fromClientName) ?: error("Could not find game client")
    }
}

enum class MappingsType(val id: String) {
    MOJANG("mojmap"),
    MCP("mcp"),
    YARN("yarn"),
    MERGED("merged");

    companion object {
        /**
         * Converts the mapping String into a [MappingsType] enum.
         *
         * @param mappings The mappings String.
         * @return The [MappingsType] corresponding [id].
         * @throws IllegalArgumentException If there is no mappings which corresponds with the [id].
         */
        @JvmStatic
        fun fromString(mappings: String) =
            enumValues<MappingsType>().find { it.id == mappings } ?: error("No such mappings: $mappings")
    }

    private fun resolvingProperty() = ReadOnlyProperty<MappingsType, _> { _, prop -> resolve(prop.name) }
    val named by resolvingProperty()
    val srg by resolvingProperty()
    val obf by resolvingProperty()
    val intermediary by resolvingProperty()

    fun resolve(type: String) = "$id-$type"
}