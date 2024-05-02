plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val toolchainTarget = JavaLanguageVersion.of(8)

repositories {
    mavenCentral()
}

base {
    archivesName.set("weave-${project.name}")
}

java {
    withSourcesJar()

    toolchain {
        languageVersion = toolchainTarget
    }
}

kotlin.jvmToolchain {
    languageVersion = toolchainTarget
}
