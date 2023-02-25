plugins {
    kotlin("jvm") version "1.8.0"
}

val projectName:    String by project
val projectVersion: String by project
val projectGroup:   String by project

group   = projectGroup
version = projectVersion

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
        "Premain-Class" to "${group}.weave.loader.WeaveLoader",
        "Can-Retransform-Classes" to true
    )
}