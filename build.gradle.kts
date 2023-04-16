plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    kotlin("plugin.lombok") version "1.8.10"

    `java-library`
    `maven-publish`
    id("com.github.weave-mc.weave-gradle") version "bcf6ab0279"
}

val projectName:    String by project
val projectVersion: String by project
val projectGroup:   String by project

group   = projectGroup
version = projectVersion

minecraft.version("1.8.9")

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    api(libs.asm)
    api(libs.asmtree)
    api(libs.asmutil)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.mixin)
    implementation(libs.kxSer)
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
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt"
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
