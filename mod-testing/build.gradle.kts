import net.weavemc.gradle.WeaveGradle
import net.weavemc.gradle.configuration.WeaveMinecraftExtension

buildscript {
    dependencies {
        classpath("net.weavemc.gradle:gradle-plugin")
    }
}

apply<WeaveGradle>()

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "net.weavemc.testingmod"
version = "unspecified"

the<WeaveMinecraftExtension>().apply {
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
    implementation("net.weavemc:internals")
    implementation("net.weavemc:loader")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    useJUnitPlatform()
}