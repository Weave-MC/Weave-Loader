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
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Weave-MC/Weave-Loader")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
            }
        }

        repositories {
            maven {
                name = "GitLabPackageRegistry"
                url = uri("https://gitlab.com/api/v4/projects/80566527/packages/maven") // https://gitlab.com/weave-mc/weave
                credentials(HttpHeaderCredentials::class) {
                    name = "Private-Token"
                    value = findProperty("gitLabPrivateToken") as String?
                }
                authentication {
                    create("header", HttpHeaderAuthentication::class)
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