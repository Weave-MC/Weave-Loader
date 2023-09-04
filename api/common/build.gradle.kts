plugins {
    id("kotlin")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.asm)
}
