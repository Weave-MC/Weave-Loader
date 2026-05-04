plugins {
    id("config-kotlin")
    id("config-publish")
}

dependencies {
    api(libs.bundles.asm)
    implementation("net.weavemc:internals")
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
            this.version = version
        }
    }
}

val chainedTasks = listOf(
    "clean",
    "build",
    "publishAllPublicationsToGitHubPackagesRepository",
    "publishAllPublicationsToGitLabPackageRegistryRepository",
    "publishAllPublicationsToLocalTestingRepository",
    "publishAllPublicationsToLocalRelativeTestingRepository",
    "publishToMavenLocal"
)

chainedTasks.forEach { taskName ->
    tasks.named(taskName) {
        // find every subproject that actually has this task and depend on it
        subprojects.forEach { sub ->
            dependsOn(sub.tasks.matching { it.name == taskName })
        }
    }
}