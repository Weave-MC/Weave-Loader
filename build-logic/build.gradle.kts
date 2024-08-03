import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "4.4.0"
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.bundles.asm)
    implementation(libs.bundles.kotlin.plugins)
    implementation(libs.gradle.shadow)
}

kotlin {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_0
        apiVersion = KotlinVersion.KOTLIN_2_0
    }
}