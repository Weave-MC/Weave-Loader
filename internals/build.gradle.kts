plugins {
    id("config-kotlin")
    id("config-publish")
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.kxser.json)
    implementation(libs.mappings)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "net.weavemc"
            artifactId = "internals"
            version = project.version.toString()
        }
    }
}