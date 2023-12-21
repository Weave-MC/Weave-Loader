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

include("loader", "api:common", "api:v1.7", "api:v1.8", "api:v1.12")