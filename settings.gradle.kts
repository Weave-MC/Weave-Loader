enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.weavemc.dev")
    }
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

includeBuild("build-logic")
include("loader", "internals", "gradle-plugin")
