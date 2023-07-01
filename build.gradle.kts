import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    kotlin("plugin.lombok") version "1.8.10"

    `java-library`
    `maven-publish`

    id("com.github.weave-mc.weave-gradle") version "bcf6ab0279"
    id("org.jetbrains.dokka") version "1.8.20"
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

tasks.withType<DokkaTask>().configureEach {
    moduleName.set("Weave Loader")
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl.set(URL("https://github.com/Weave-MC/Weave-Loader/tree/master/src"))
            remoteLineSuffix.set("#L")
        }
        externalDocumentationLink("https://kotlinlang.org/api/kotlinx.serialization/", "https://kotlinlang.org/api/kotlinx.serialization/package-list")
        externalDocumentationLink("https://asm.ow2.io/javadoc/", "https://asm.ow2.io/javadoc/element-list")
        externalDocumentationLink("https://jenkins.liteloader.com/job/Mixin/javadoc/","https://jenkins.liteloader.com/job/Mixin/javadoc/package-list")
        externalDocumentationLink("https://projectlombok.org/api/","https://projectlombok.org/api/element-list")
        externalDocumentationLink("https://javadoc.io/doc/org.jetbrains/annotations/13.0/","https://javadoc.io/doc/org.jetbrains/annotations/13.0/package-list")
    }
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
