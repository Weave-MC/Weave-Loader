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

kotlin.compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")

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