import gradle.kotlin.dsl.accessors._d73e0aad27c892ff134862f94ec3182c.base
import gradle.kotlin.dsl.accessors._d73e0aad27c892ff134862f94ec3182c.java
import gradle.kotlin.dsl.accessors._d73e0aad27c892ff134862f94ec3182c.kotlin
import org.gradle.kotlin.dsl.`java-library`
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val toolchainTarget = Action<JavaToolchainSpec> {
    languageVersion.set(JavaLanguageVersion.of(8))
}

repositories {
    mavenCentral()
}

base {
    archivesName.set("weave-${project.name}")
}

java.withSourcesJar()

toolchainTarget.execute(java.toolchain)
kotlin.jvmToolchain(toolchainTarget)