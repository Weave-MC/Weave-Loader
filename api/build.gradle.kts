plugins {
    id("config-kotlin")
    `maven-publish`
}

dependencies {
    api(libs.bundles.asm)
    api(libs.mappingsUtil)
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
            artifactId = "common"
            version = "${project.version}"
        }
    }
}