plugins {
    // Because of kotlin-dsl, gradle will be upset because of double application of the kotlin plugin
    // not like we have a choice to omit it though
    `kotlin-dsl`
    alias(libs.plugins.serialization.dsl)
    id("config-publish")
}

kotlin { jvmToolchain(8) }

dependencies {
    implementation(libs.bundles.asm)
    implementation(libs.kxser.json)
    implementation(libs.mappings)
    implementation(project(":internals"))
}

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
    }
}

gradlePlugin {
    plugins {
        val weave by creating {
            id = "net.weavemc.gradle"
            displayName = "Weave-Gradle"
            description = "Provides various utilities for Weave modders to develop and package their mods"
            implementationClass = "net.weavemc.gradle.WeaveGradle"
        }
    }
}

base {
    archivesName = "Weave-Gradle"
}