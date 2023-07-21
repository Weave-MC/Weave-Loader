plugins {
    id("com.github.weave-mc.weave-gradle")
}

minecraft.version("1.7.10")

dependencies {
    implementation(project(":api:common"))
    implementation(libs.bundles.asm)
}
