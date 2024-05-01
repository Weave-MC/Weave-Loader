plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val toolchainTarget = Action<JavaToolchainSpec> {
    languageVersion.set(JavaLanguageVersion.of(8))
}

repositories {
    mavenCentral()
}

java.withSourcesJar()

toolchainTarget.execute(java.toolchain)
kotlin.jvmToolchain(toolchainTarget)