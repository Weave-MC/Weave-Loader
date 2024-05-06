plugins {
    java
    id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
}

val minecraftVersion: String = project.name.substringAfterLast("-")

minecraft {
    version(minecraftVersion)
    runs {
        client()
    }
}

val runClient by tasks.getting(JavaExec::class) {
    val weaveLoader = tasks.getByPath(":loader:shadowJar").outputs.files.asPath
    val weaveTestMod = tasks.getByPath(":integrationTests:testmod:jar").outputs.files.asPath

//    println("Weave Loader: $weaveLoader")
//    println("Weave Test Mod: $weaveTestMod")
    // Load Weave Loader with the single test mod as an argument
    this.jvmArgs("-javaagent:$weaveLoader=\"$weaveTestMod\"")
}
runClient.dependsOn(":loader:shadowJar")
runClient.dependsOn(":integrationTests:testmod:jar")