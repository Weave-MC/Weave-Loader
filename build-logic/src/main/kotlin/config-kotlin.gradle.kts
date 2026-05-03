import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

base {
    archivesName = "weave-${project.name}"
}

java.withSourcesJar()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        languageVersion = KotlinVersion.KOTLIN_2_3
        apiVersion = KotlinVersion.KOTLIN_2_3
    }

    jvmToolchain(8)
}
