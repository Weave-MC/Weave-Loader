plugins {
    id("config-kotlin")
    `java-gradle-plugin`
    id("config-publish")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    implementation(libs.bundles.asm)
    implementation(libs.kxser.json)
    implementation(libs.mappings)
    implementation(projects.internals)
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

base.archivesName = "Weave-Gradle"