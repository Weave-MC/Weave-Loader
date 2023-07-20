repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    compileOnly(rootProject.libs.mixin)
    implementation(project(":api"))
}
