plugins {
    application
    id("com.github.weave-mc.weave-gradle") version "bcf6ab0279" apply false
    kotlin("plugin.serialization") version "1.8.10"
    kotlin("plugin.lombok") version "1.8.10"
}

repositories {
    maven("https://jitpack.io")
}

subprojects {
    apply(plugin = "com.github.weave-mc.weave-gradle")
    repositories.maven("https://jitpack.io")
    dependencies.compileOnly(this.project.path)
}
