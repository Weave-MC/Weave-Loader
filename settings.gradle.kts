val projectName: String by settings
rootProject.name = projectName

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://jitpack.io")
    }
}
