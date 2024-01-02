plugins {
    id("net.weavemc.gradle")
    `java-library`
    `maven-publish`
    id("relocate")
    id("kotlin")
}

minecraft {
    version("1.8.9")
    mappings("mcp")
}

dependencies {
    api(project(":api:common"))
    api(libs.bundles.asm)
    compileOnly(libs.weaveInternals)
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
