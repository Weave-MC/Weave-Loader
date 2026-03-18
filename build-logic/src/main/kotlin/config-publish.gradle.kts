plugins {
    `maven-publish`
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

        maven {
            name = "LocalTesting"
            url = uri("${System.getProperty("user.home")}/.weave/testRepo")
        }

        maven {
            name = "LocalRelativeTesting"
            url = layout.buildDirectory.dir("localMaven").get().asFile.toURI()
        }
    }
}