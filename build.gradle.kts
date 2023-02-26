plugins {
    kotlin("jvm") version "1.8.0"
}

group = "club.maxstats"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Weave-MC:Weave-Api:eaa143c17b")
}

kotlin {
    jvmToolchain(8)
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { zipTree(it) }) {
        exclude("**/module-info.class")
    }

    manifest.attributes(
        "Premain-Class" to "club.maxstats.weave.loader.WeaveLoader",
        "Can-Retransform-Classes" to true
    )
}