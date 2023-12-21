allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://repo.weavemc.dev/releases")
        }
    }
}