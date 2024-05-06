import net.weavemc.gradle.configuration.WeaveMinecraftExtension

plugins {
    id("net.weavemc.gradle") version "1.0.0-PRE"
}

configure<WeaveMinecraftExtension> {
    configure {
        name = "Test Mod"
        modId = "testmod"
//        entryPoints = listOf("com.example.mod.ExampleMod")
//        mixinConfigs = listOf("examplemod.mixins.json")
        mcpMappings()
    }
    version("1.8.9")
}