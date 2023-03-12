plugins {
    kotlin("jvm") version "1.8.0"
    `java-library`
    `maven-publish`
    id("com.github.weave-mc.weave") version "8b70bcc707"
}

group = "club.maxstats"
version = "1.0.0"

minecraft.version("1.8.9")

repositories {
    mavenCentral()
}

dependencies {
    api("org.ow2.asm:asm:9.4")
    api("org.ow2.asm:asm-tree:9.4")
    api("org.ow2.asm:asm-util:9.4")
}

kotlin.jvmToolchain(11)

val agent by tasks.registering(Jar::class) {
    archiveAppendix.set("Agent")
    group = "build"

    from(sourceSets.main.get().output)
    from({ configurations.runtimeClasspath.get().map { zipTree(it) } }) {
        exclude("**/module-info.class")
    }

    manifest.attributes(
        "Premain-Class" to "club.maxstats.weave.loader.bootstrap.AgentKt"
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
