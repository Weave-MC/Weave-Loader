plugins {
    id("com.github.weave-mc.weave-gradle")
    id("kotlin")
}

minecraft.version("1.12.2")

dependencies {
    api(project(":api:common"))
}
