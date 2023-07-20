pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}

val projectName: String by settings
rootProject.name = projectName

include("api")
include("loader")

include("api:v1.7")
findProject(":api:v1.7")?.name = "v1.7"
include("api:v1.8")
findProject(":api:v1.8")?.name = "v1.8"
include("api:v1.12")
findProject(":api:v1.12")?.name = "v1.12"
