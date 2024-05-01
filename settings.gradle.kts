pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.weavemc.dev")
        mavenLocal()
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.+"
        id("net.weavemc.gradle") version "1.0.0"
    }
}

val projectName: String by settings
rootProject.name = projectName

include("loader", "api")