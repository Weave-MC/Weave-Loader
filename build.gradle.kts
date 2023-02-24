plugins {
    kotlin("jvm") version "1.8.0"
}

group = "club.maxstats"
version = "1.0"

repositories.mavenCentral()

dependencies {
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-tree:9.4")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
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