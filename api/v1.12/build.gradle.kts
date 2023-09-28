plugins {
    id("com.github.weave-mc.weave-gradle")
    `maven-publish`
}

minecraft.version("1.12.2")

dependencies {
    api(project(":api:common"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "net.weavemc.api"
            artifactId = "1.12"
            version = "1.0"
        }
    }

    repositories {
        mavenLocal()
    }
}
