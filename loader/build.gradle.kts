plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.lombok")
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    compileOnly(libs.mixin)
    implementation(libs.bundles.asm)
    implementation(libs.kxSer)
    implementation(project(":api:common"))
}

tasks.jar {
    from({ configurations.runtimeClasspath.get().map { zipTree(it) } }) {
        exclude("**/module-info.class")
    }

    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt"
    )
}
