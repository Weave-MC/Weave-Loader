@file:Suppress("VulnerableLibrariesLocal")

import java.util.*

plugins {
    id("config-kotlin")
    id("config-shade")
    id("config-publish")
}

repositories {
    maven("https://maven.fabricmc.net/")
}

kotlin {
    compilerOptions {
        explicitApi()
        optIn.add("net.weavemc.loader.impl.bootstrap.PublicButInternal")
    }
}

dependencies {
    shade(projects.internals)
    shade(projects.api)
    shade(libs.klog)
    shade(libs.kxser.json)
    shade(libs.bundles.asm)
    shade(libs.bundles.maven.resolver)
    shade(libs.mappings)
    shade(libs.mixin) {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
    }
}

tasks {
    abstract class AddWeaveLoaderPropertiesTask : DefaultTask() {
        @get:Input
        abstract val version: Property<String>

        @get:OutputFile
        val propertiesFile: RegularFileProperty =
            project.objects.fileProperty().convention(
                project.layout.buildDirectory.file("tmp/weave-loader-data.properties")
            )

        @TaskAction
        fun run() {
            val properties = propertiesOf(
                "version" to version.get(),
            )

            propertiesFile.get().asFile.outputStream().use {
                properties.store(it, "Weave Loader Properties")
            }
        }

        private fun propertiesOf(vararg props: Pair<String, Any?>) =
            Properties().also { it += props }
    }

    val addWeaveLoaderProperties by registering(AddWeaveLoaderPropertiesTask::class) {
        this.version = version
    }

    shadowJar {
        from(addWeaveLoaderProperties)
    }

    jar {
        manifest.attributes(
            "Premain-Class" to "net.weavemc.loader.impl.bootstrap.AgentKt",
            "Main-Class" to "net.weavemc.loader.impl.bootstrap.AgentKt",
            "Can-Retransform-Classes" to "true",
        )

        manifest.attributes(
            mapOf(
                "Specification-Title" to "Weave Loader",
                "Specification-Version" to "0", // we're still in beta, so this is 0
                "Specification-Vendor" to "WeaveMC",
                "Implementation-Title" to "Weave Loader",
                "Implementation-Version" to version,
                "Implementation-Vendor" to "WeaveMC",
            ), "net.weavemc.loader.impl"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "net.weavemc"
            artifactId = "loader"
            this.version = version
        }
    }
}