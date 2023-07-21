plugins {
    id("com.github.weave-mc.weave-gradle")
}

minecraft.version("1.12.2")

dependencies {
    implementation(project(":api:common"))
}
