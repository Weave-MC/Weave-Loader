plugins {
    kotlin("jvm") version "1.8.0"
    `java-library`
    `maven-publish`
    id("com.github.weave-mc.weave") version "8b70bcc707"
}

val projectName:    String by project
val projectVersion: String by project
val projectGroup:   String by project

group   = projectGroup
version = projectVersion

minecraft.version("1.8.9")

repositories {
    mavenCentral()
}

dependencies {
    api(libs.asm)
    api(libs.asmtree)
    api(libs.asmutil)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

kotlin {
    jvmToolchain(8)
    explicitApiWarning()
}

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
