pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.weavemc.dev")
        mavenLocal()
    }
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.+"
}

rootProject.name = "weave-loader"

includeBuild("build-logic")
