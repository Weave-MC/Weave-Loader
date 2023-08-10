import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0" apply false
    kotlin("plugin.serialization") version "1.9.0" apply false
    kotlin("plugin.lombok") version "1.9.0" apply false
}

subprojects {
    tasks.withType<JavaCompile> {
        targetCompatibility = "1.8"
        sourceCompatibility = "1.8"
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
