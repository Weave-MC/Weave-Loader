allprojects {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://repo.weavemc.dev/releases")
        }
    }
}