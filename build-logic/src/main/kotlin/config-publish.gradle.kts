import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "WeaveReleases"
            url = uri("https://repo.weavemc.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}