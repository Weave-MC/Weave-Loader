plugins {
    id("config-kotlin")
    id("config-publish")
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.internals)
    implementation(libs.kxser.json)
    implementation(libs.mappings)
}

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "net.weavemc.api"
            artifactId = project.name
            version = libs.versions.api.get().toString()
        }
    }
}