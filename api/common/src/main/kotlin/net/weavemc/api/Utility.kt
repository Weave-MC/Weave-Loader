@file:JvmName("Utility")

package net.weavemc.api

val args: Map<String, String>
    get() = System.getProperties()["weave.main.args"] as? Map<String, String>
        ?: error("Failed to retrieve Minecraft arguments")

val sunArgs = System.getProperty("sun.java.command")
    ?: error("Failed to retrieve command line arguments, this should never happen.")

private val versionString: String by lazy {
    args["version"]?.lowercase() ?: error("Could not parse version from arguments")
}

val gameVersion: MinecraftVersion by lazy {
    versionString.let(MinecraftVersion::fromVersionName)
        ?: error("Could not find game version")
}
val gameClient: MinecraftClient by lazy {
    when {
        classExists("com.moonsworth.lunar.genesis.Genesis") -> MinecraftClient.LUNAR
        versionString.contains("forge") -> MinecraftClient.FORGE
        versionString.contains("labymod") -> MinecraftClient.LABYMOD
        else -> MinecraftClient.VANILLA
    }
}
val gameLauncher: MinecraftLauncher by lazy {
    when {
        classExists("org.multimc.EntryPoint") -> MinecraftLauncher.MULTIMC
        classExists("org.prismlauncher.EntryPoint") -> MinecraftLauncher.PRISM
        else -> MinecraftLauncher.OTHER
    }
}

private fun classExists(name: String): Boolean =
    MinecraftLauncher::class.java.classLoader.getResourceAsStream("${name.replace('.', '/')}.class") != null