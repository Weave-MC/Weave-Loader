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

        maven {
            name = "GitLabPackageRegistry"
            val projectId = System.getenv("CI_PROJECT_ID") ?: "80566527"
            url = uri("${System.getenv("CI_API_V4_URL") ?: "https://gitlab.com/api/v4"}/projects/$projectId/packages/maven")

            credentials(HttpHeaderCredentials::class) {
                val isCi = System.getenv("CI_JOB_TOKEN") != null
                name = if (isCi) "Job-Token" else "Private-Token"
                value = System.getenv("CI_JOB_TOKEN") ?: (findProperty("gitLabPrivateToken") as String?)
            }

            authentication {
                create<HttpHeaderAuthentication>("header")
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