allprojects {
    val projectGroup: String by project
    val projectVersion: String by project

    group = projectGroup
    version = projectVersion

    repositories {
        mavenCentral()
        /*maven {
            url = uri("https://repo.weavemc.dev/releases")
        }*/
        maven("https://gitlab.com/api/v4/projects/64882766/packages/maven") // Weave Internals
    }
}