pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }

    plugins {
        id("com.github.weave-mc.weave-gradle") version "bcf6ab0279"
    }
}

val projectName: String by settings
rootProject.name = projectName

include("loader", "api:common", "api:v1.7", "api:v1.8", "api:v1.12")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}