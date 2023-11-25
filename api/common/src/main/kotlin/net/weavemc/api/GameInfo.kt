package net.weavemc.api

data class GameInfo(
    val version: Version,
    val client: Client,
) {
    enum class Version(
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
            fun fromProtocol(protocol: Int): Version? = entries.find { it.protocol == protocol }
            fun fromVersionName(versionName: String): Version? =
                entries.find { it.versionName == versionName } ?: entries.find { it.aliases.contains(versionName) }

            fun fromAlias(alias: String): Version? = entries.find { it.aliases.contains(alias) }
        }
    }

    enum class Client(
        val clientName: String,
        vararg val aliases: String,
    ) {
        VANILLA("Vanilla"),
        FORGE("Forge", "MinecraftForge", "Minecraft Forge"),
        LABYMOD("LabyMod", "Laby"),
        LUNAR("Lunar Client", "Lunar", "LunarClient"),
        BADLION("Badlion Client", "BLC", "Badlion", "BadlionClient");

        companion object {
            fun fromClientName(clientName: String): Client? =
                entries.find { it.clientName.equals(clientName, ignoreCase = true) }
                    ?: entries.find { it.aliases.any { alias -> alias.equals(clientName, ignoreCase = true) } }
        }
    }
}
