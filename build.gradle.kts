subprojects {
    val projectGroup: String by project
    val projectVersion: String by project

    group = projectGroup
    version = projectVersion

    repositories {
        mavenCentral()
        maven("https://repo.weavemc.dev/releases")
    }
}