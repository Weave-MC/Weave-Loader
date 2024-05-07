import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

java.withSourcesJar()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
    }
    jvmToolchain(8)
}
