plugins {
    id("net.weavemc.gradle")
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
            artifactId = "1.7"
            version = "1.0.0"
        }
    }
}
