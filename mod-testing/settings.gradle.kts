dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mod-testing"
includeBuild("..") {
    dependencySubstitution {
        substitute(module("net.weavemc:internals")).using(project(":internals"))
        substitute(module("net.weavemc:loader")).using(project(":loader"))
        substitute(module("net.weavemc.gradle:gradle-plugin")).using(project(":gradle-plugin"))
    }
}