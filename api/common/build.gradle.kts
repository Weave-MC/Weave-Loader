plugins {
    id("kotlin")
    `maven-publish`
}

dependencies {
    api(libs.bundles.asm)
    api(libs.mappingsUtil)
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
            artifactId = "common"
            version = "1.0.0"
        }
    }
}