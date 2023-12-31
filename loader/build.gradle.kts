plugins {
    `maven-publish`
    id("kotlin")
    id("shade")
    id("relocate")
}

dependencies {
    implementation(libs.kxSer)
    api(libs.bundles.asm)
    api(project(":api:common"))
    api(libs.weaveIntermediary)
}

tasks.jar {
    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt",
        "Can-Retransform-Classes" to "true"
    )
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
            groupId = "net.weavemc"
            artifactId = "loader"
            version = "1.0.0"
        }
    }
}
