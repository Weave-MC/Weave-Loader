plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization.dsl) apply false
}

subprojects {
    group = "net.weavemc"
    version = "1.0.0-b.3"

    repositories {
        mavenCentral()
        maven("https://repo.weavemc.dev/releases")
    }
}