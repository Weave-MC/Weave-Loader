plugins {
    `maven-publish`
    id("net.weavemc.gradle")
    id("kotlin")
}

weavecraft.version("1.12.2")

dependencies {
    api(project(":api:common"))
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
            artifactId = "1.12"
            version = "1.0.0"
        }
    }
}
