plugins {
    id("net.weavemc.gradle") version "1.0.0-PRE"
}

group = "net.weavemc.loader"
version = "1.0.0"

minecraft {
    configure {
        name = "Test Mod"
        modId = "testmod"
        entryPoints = listOf("net.weavemc.loader.tests.TestMod")
        mojangMappings()
    }
    version("1.16.5")
}

dependencies {
    implementation(project(":loader"))
}

// Workaround 1.0.0-PRE bug
tasks["remapJar"].enabled = false
tasks["processResources"].outputs.upToDateWhen { false }