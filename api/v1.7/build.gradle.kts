plugins {
    id("com.github.weave-mc.weave-gradle")
    `java-library`
    `maven-publish`
    id("relocate")
    id("kotlin")
}

minecraft.version("1.7.10")

dependencies {
    api(project(":api:common"))
    api(libs.bundles.asm)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "net.weavemc.api"
            artifactId = "1.7"
            version = "1.0"
        }
    }

    repositories {
        mavenLocal()
    }
}
