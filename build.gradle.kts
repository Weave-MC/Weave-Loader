import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.8.0"
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("com.github.weave-mc.weave") version "3ad11a0fd5"
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
    jvmToolchain(11)
}

minecraft {
    version = "1.8.9"
    mappings = "stable_22"
}

val agent by tasks.creating(ShadowJar::class) {
    archiveAppendix.set("Agent")
    group = "build"

    from(sourceSets.main.get().output)
    configurations += project.configurations.runtimeClasspath.get()

    manifest.attributes(
        "Premain-Class" to "club.maxstats.weave.loader.WeaveLoader"
    )
}

tasks.build {
    dependsOn(agent)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}