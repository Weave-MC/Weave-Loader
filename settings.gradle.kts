pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}

val projectName: String by settings
rootProject.name = projectName

include("loader")

include("api:common")
include("api:v1.7")
include("api:v1.8")
include("api:v1.12")
