plugins {
    id("com.github.weave-mc.weave-gradle")
    `java-library`
    `maven-publish`
}

minecraft.version("1.8.9")

dependencies {
    api(project(":api:common"))
    api(libs.bundles.asm)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "net.weavemc.api"
            artifactId = "1.8"
            version = "1.0"
        }
    }

    repositories {
        mavenLocal()
    }
}
