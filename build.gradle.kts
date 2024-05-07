plugins {
    id("config-kotlin")
    id("config-shade")
    `maven-publish`
}

group = properties["projectGroup"] as String
version = properties["projectVersion"] as String

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.weavemc.dev/releases")
}

dependencies {
    shade(libs.klog)
    shade(libs.kxSer)
    shade(libs.bundles.asm)
    shade(libs.weaveInternals)
    shade(libs.mappingsUtil)
    shade(libs.mixin) {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
    }
}

tasks {
    val apiJar by creating(Jar::class) {
        archiveClassifier.set("api")
        from(sourceSets["main"].output) {
            include("net/weavemc/api/**")
        }
        from("LICENSE")
    }
    jar {
        manifest.attributes(
            "Premain-Class" to "net.weavemc.loader.impl.bootstrap.AgentKt",
            "Main-Class" to "net.weavemc.loader.impl.bootstrap.AgentKt",
            "Can-Retransform-Classes" to "true",
        )
        from("LICENSE")
    }
    project.artifacts.archives(apiJar)
}

//TODO: Better publications
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
