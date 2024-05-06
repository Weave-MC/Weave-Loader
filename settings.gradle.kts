pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://repo.weavemc.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.+"
}

val projectName: String by settings
rootProject.name = projectName

includeBuild("build-logic")
include("loader", "api")

val legacyVersions = arrayOf(
    "1.7.10", "1.8.9", "1.9.4", "1.10.2", "1.11.2", "1.12.2"
)
val modernVersions = arrayOf(
    "1.14.4", "1.16.5", "1.17.1", "1.18.2", "1.19.4", "1.20.6"
)

val integrationTests = mapOf(
    // VanillaGradle supports both modern mojmapped versions
    "vanilla" to listOf(*modernVersions),

    // Split fabric into two groups since support for legacy is not official
    "fabric" to listOf(*modernVersions),
    "legacy-fabric" to listOf(*legacyVersions),

    // Forge can support both legacy and modern versions
    "forge" to listOf(*legacyVersions, *modernVersions)
)

include("integrationTests:testmod")
integrationTests.keys.forEach { loader ->
    integrationTests[loader]?.forEach { version ->
        include("integrationTests:$loader:$version")
        project(":integrationTests:$loader:$version").apply {
            projectDir = file("integrationTests/$loader/$version").also { it.mkdirs() }
            buildFileName = "../build.gradle.kts"
        }
    }
}
