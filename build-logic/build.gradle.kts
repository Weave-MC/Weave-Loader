import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `kotlin-dsl`
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
    jvmToolchain(17)

    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_3
        apiVersion = KotlinVersion.KOTLIN_2_3
    }
}