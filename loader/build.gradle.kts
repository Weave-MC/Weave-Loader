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
    implementation(libs.mixin)
    implementation(libs.kxSer)
    implementation("com.google.guava:guava:21.0")
    api(libs.bundles.asm)
    api(project(":api:common"))
}

tasks.jar {
    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt",
        "Can-Retransform-Classes" to "true"
    )
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
