plugins {
    id("kotlin")
    `maven-publish`
    relocate
}

dependencies {
    api(libs.bundles.asm)
    api(libs.mappingsUtil)
    compileOnly(libs.weaveInternals)
}

publishing {
    repositories {
        /*maven {
            name = "WeaveMC"
            url = uri("https://repo.weavemc.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }*/

        maven {
            url = uri("https://gitlab.com/api/v4/projects/64882633/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = findProperty("gitLabPrivateToken") as String?
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
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