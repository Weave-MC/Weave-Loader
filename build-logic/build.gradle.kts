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
    implementation(libs.dokka.core)
    implementation(libs.dokka.javadoc)
}

kotlin {
    jvmToolchain(17)
}