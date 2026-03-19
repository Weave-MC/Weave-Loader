tasks.register("clean") {
    group = "build"
    description = "Cleans all included builds"
    dependsOn(gradle.includedBuilds.map { it.task(":clean") })
}

tasks.register("build") {
    group = "build"
    description = "Builds all included builds"
    dependsOn(gradle.includedBuilds.map { it.task(":build") })
}

tasks.register("publish") {
    group = "publishing"
    description = "Publishes all included builds"
    dependsOn(gradle.includedBuilds.map { it.task(":publish") })
}

val publishableProjects = listOf("internals", "api", "loader", "gradle-plugin")

tasks.register("publishToGitHubPackages") {
    group = "publishing"
    publishableProjects.forEach { name ->
        gradle.includedBuilds.find { it.name == name }?.task(":publishAllPublicationsToGitHubPackagesRepository")?.let {
            dependsOn(it)
        }
    }
}

// LocalTesting (~/.weave/testRepo)
tasks.register("publishToLocalTesting") {
    group = "publishing"
    publishableProjects.forEach { name ->
        gradle.includedBuilds.find { it.name == name }?.task(":publishAllPublicationsToLocalTestingRepository")?.let {
            dependsOn(it)
        }
    }
}

// LocalRelativeTesting (build/localMaven)
tasks.register("publishToLocalRelative") {
    group = "publishing"
    publishableProjects.forEach { name ->
        gradle.includedBuilds.find { it.name == name }?.task(":publishAllPublicationsToLocalRelativeTestingRepository")?.let {
            dependsOn(it)
        }
    }
}

tasks.register("publishToMavenLocal") {
    group = "publishing"
    publishableProjects.forEach { name ->
        gradle.includedBuilds.find { it.name == name }?.task(":publishToMavenLocal")?.let {
            dependsOn(it)
        }
    }
}