plugins {
    id("kotlin")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.bundles.asm)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "net.weavemc.api"
            artifactId = "common"
            version = "1.0"
        }
    }

    repositories {
        mavenLocal()
    }
}
