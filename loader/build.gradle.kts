plugins {
    `maven-publish`
    id("kotlin")
    id("shade")
    id("relocate")
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.kxSer)
    implementation(libs.bundles.asm)
    implementation(project(":api"))
    implementation(libs.weaveInternals)
    implementation(libs.mappingsUtil)
    implementation(libs.bundles.mixin)
}

tasks.jar {
    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt",
        "Can-Retransform-Classes" to "true",
        "Main-Class" to "net.weavemc.loader.bootstrap.AgentKt"
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
            version = "${project.version}-PRE"
        }
    }
}
