enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Weave-Loader"

fun ConfigurableIncludedBuild.setDependencySubstitution(module: String) {
    dependencySubstitution {
        substitute(module(module))
            .using(project(":"))
    }
}

includeBuild("build-logic")
includeBuild("internals") {
    setDependencySubstitution("net.weavemc:internals")
}
includeBuild("api") {
    setDependencySubstitution("net.weavemc.api:api")
}
includeBuild("loader") {
    setDependencySubstitution("net.weavemc:loader")
}
includeBuild("gradle-plugin")
includeBuild("mod-testing")