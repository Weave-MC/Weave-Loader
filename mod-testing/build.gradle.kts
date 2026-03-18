plugins {
    alias(libs.plugins.kotlin.jvm)
    id("net.weavemc.gradle")
}

group = "net.weavemc.testingmod"
version = "unspecified"

weave {
    configure {
        name = "TestingMod"
        modId = "net.weavemc.testingmod"
        entryPoints = listOf("net.weavemc.testingmod.TestingMod")
        mixinConfigs = listOf("testingmod.mixins.json")
        mcpMappings()
    }
    version("1.8.9")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.internals)
    implementation(libs.loader)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
}