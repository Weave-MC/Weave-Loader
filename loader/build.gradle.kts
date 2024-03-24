plugins {
    `maven-publish`
    id("kotlin")
    id("shade")
    id("relocate")
}

repositories {
    // TODO: standardize help this is such a mess
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.kxSer)
    api(libs.bundles.asm)
    compileOnly(project(":api:common"))
    api(libs.weaveInternals)
    api(libs.mappingsUtil)

    // TODO: as libs.toml
    api("org.spongepowered:mixin:0.8.5")
    implementation("com.google.guava:guava:21.0")
    implementation("com.google.code.gson:gson:2.2.4")
}

tasks.jar {
    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt",
        "Can-Retransform-Classes" to "true"
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
            version = "1.0.0"
        }
    }
}
