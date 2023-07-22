plugins {
    id("com.github.weave-mc.weave-gradle")
    `java-library`
}

minecraft.version("1.8.9")

dependencies {
    api(project(":api:common"))
    api(libs.bundles.asm)
}
