plugins {
    id("config-kotlin")
    id("config-publish")
}

allprojects {
    group = "net.weavemc"
    version = libs.versions.weave.get()
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
            this.version = version
        }
    }
}