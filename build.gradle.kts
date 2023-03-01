plugins {
    kotlin("jvm") version "1.8.0"
    `java-library`
    `maven-publish`
    id("com.github.weave-mc.weave") version "e533e7846c"
}

group = "club.maxstats"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    api("org.ow2.asm:asm:9.4")
    api("org.ow2.asm:asm-tree:9.4")
}

kotlin {
    jvmToolchain(8)
}

minecraft {
    version = "1.8.9"
    mappings = "stable_22"
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}