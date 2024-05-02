plugins {
    id("config-kotlin")
    id("config-shade")
    `maven-publish`
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    shade(libs.kxSer)
    shade(libs.bundles.asm)
    shade(project(":api"))
    shade(libs.weaveInternals)
    shade(libs.mappingsUtil)
    shade(libs.bundles.mixin)
}

tasks.jar {
    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt",
        "Main-Class" to "net.weavemc.loader.bootstrap.AgentKt",
        "Can-Retransform-Classes" to "true",
    )
}

publishing {
    repositories {
        maven("https://repo.weavemc.dev/releases") {
            name = "WeaveMC"
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
            version = "${project.version}"
        }
    }
}