import com.github.javaparser.printer.concretesyntaxmodel.CsmElement.token

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

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Weave-MC/Weave-Loader")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
                }
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