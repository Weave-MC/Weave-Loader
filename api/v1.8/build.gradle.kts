plugins {
    id("com.github.weave-mc.weave-gradle")
}

minecraft.version("1.8.9")

dependencies {
    api(project(":api:common"))
}
