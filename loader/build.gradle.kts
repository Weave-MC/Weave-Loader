plugins {
    id("config-kotlin")
    id("config-shade")
    id("config-publish")
}

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    shade(project(":api"))
    shade(libs.kxser.json)
    shade(libs.bundles.asm)
    shade(project(":internals"))
    shade(libs.mappings)
    shade(libs.mixin) {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
    }
}

tasks {
    jar {
        manifest.attributes(
            "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt",
            "Main-Class" to "net.weavemc.loader.bootstrap.AgentKt",
            "Can-Retransform-Classes" to "true",
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "net.weavemc"
            artifactId = "loader"
            version = "${project.version}"
        }
    }
}