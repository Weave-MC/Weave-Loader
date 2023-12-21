plugins {
    id("net.weavemc.gradle")
    `java-library`
    `maven-publish`
    id("relocate")
    id("kotlin")
}

weavecraft.version("1.8.9")

dependencies {
    api(project(":api:common"))
    api(libs.bundles.asm)
}

publishing {
    repositories {
        maven {
            name = "WeaveMC"
            url = uri("https://repo.weavemc.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "net.weavemc.api"
            artifactId = "1.8"
            version = "1.0.0"
        }
    }
}
