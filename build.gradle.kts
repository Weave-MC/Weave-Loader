allprojects {
    val projectGroup: String by project
    val projectVersion: String by project

    group = projectGroup
    version = projectVersion

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://repo.weavemc.dev/releases")
        }
    }
}