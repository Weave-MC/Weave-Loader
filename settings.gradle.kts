pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.weavemc.dev")
        mavenLocal()
    }

    plugins {
        id("net.weavemc.gradle") version "1.0.0"
    }
}

val projectName: String by settings
rootProject.name = projectName

include("loader", "api")