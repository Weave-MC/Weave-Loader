import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

base {
    archivesName = "weave-${project.name}"
}

java.withSourcesJar()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        languageVersion = KotlinVersion.KOTLIN_1_9
        apiVersion = KotlinVersion.KOTLIN_1_9
    }

    jvmToolchain(8)
}
