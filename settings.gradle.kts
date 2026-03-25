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

includeBuild("build-logic")
includeBuild("internals") {
    dependencySubstitution {
        substitute(module("net.weavemc:internals"))
            .using(project(":"))
    }
}
includeBuild("api") {
    dependencySubstitution {
        substitute(module("net.weavemc.api:api"))
            .using(project(":"))
    }
}
includeBuild("loader") {
    dependencySubstitution {
        substitute(module("net.weavemc:loader"))
            .using(project(":"))
    }
}
includeBuild("gradle-plugin")
includeBuild("mod-testing")