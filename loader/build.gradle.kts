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

tasks.shadowJar {
    mergeServiceFiles()
    relocate("org.objectweb.asm", "net.weavemc.asm") {
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

