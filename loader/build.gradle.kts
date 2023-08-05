import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.lombok")
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.mixin)
    implementation(libs.kxSer)
    api(libs.bundles.asm)
    api(project(":api:common"))
}

val mixins: SourceSet by sourceSets.creating

sourceSets.main {
    compileClasspath += mixins.output
}

configurations {
    mixins.compileClasspathConfigurationName {
        extendsFrom(compileClasspath.get())
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({ configurations.runtimeClasspath.get().map { zipTree(it) } }) {
        exclude("**/module-info.class")
    }

    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt"
    )
}

tasks.assemble {
    dependsOn("shadowJar")
}

val relocatedJar by tasks.registering(ShadowJar::class) {
    archiveClassifier.set("relocated")
    from({sourceSets.main.get().output})
    configurations = listOf(project.configurations.runtimeClasspath.get())
    mergeServiceFiles()
    relocate("org.objectweb.asm", "net.weavemc.asm")
}

tasks.shadowJar {
    from(relocatedJar)
    from(mixins.output)
    archiveClassifier.set("agent")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

