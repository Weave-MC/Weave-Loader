plugins {
    java
    id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
}

val minecraftVersion: String = "1.16.5"

minecraft {
    version(minecraftVersion)
    runs {
        client()
    }
}

val runClient by tasks.getting(JavaExec::class) {
//        println(this.javaClass)
//        doFirst {
//            val weaveLoader = project("loader").tasks["shadowJar"].outputs.files.asPath
//            val weaveTestMod = project("integrationTests:testmod").tasks["jar"].outputs.files.asPath
//
//            println("Weave Loader: $weaveLoader")
//            println("Weave Test Mod: $weaveTestMod")
//            this@getting.jvmArgs("-javaagent:$weaveLoader=args")
//        }
    dependsOn(project(":loader").tasks["shadowJar"])
    dependsOn(project(":integrationTests:testmod").tasks["jar"])
}