plugins {
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.bundles.asm)
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
