import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
}

repositories {
    mavenCentral()
}

base {
    archivesName = "weave-${project.name}"
}

java {
    withSourcesJar()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        languageVersion = KotlinVersion.KOTLIN_2_3
        apiVersion = KotlinVersion.KOTLIN_2_3
    }

    jvmToolchain(8)

    explicitApi()
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}