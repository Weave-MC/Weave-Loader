@file:Suppress("VulnerableLibrariesLocal")

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
    shade(libs.klog)
    shade(libs.kxser.json)
    shade(libs.bundles.asm)
    shade(libs.mappings)
    shade(libs.mixin) {
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
    }
}

tasks.jar {
    exclude("net/weavemc/loader/api/**")
    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.impl.bootstrap.AgentKt",
        "Main-Class" to "net.weavemc.loader.impl.bootstrap.AgentKt",
        "Can-Retransform-Classes" to "true",

        "Automatic-Module-Name" to "net.weavemc.loader",
    )

    manifest.attributes(
        mapOf(
            "Specification-Title" to "Weave Loader",
            "Specification-Version" to "0", // we're still in beta so this is 0
            "Specification-Vendor" to "WeaveMC",
            "Implementation-Title" to "Weave Loader",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "WeaveMC",
        ), "net.weavemc.loader"
    )
}

val apiJar by tasks.creating(Jar::class) {
    archiveClassifier.set("api")
    from(sourceSets.main.get().output)
    include("net/weavemc/loader/api/**")

    manifest.attributes(
        mapOf(
            "Specification-Title" to "Weave Loader API",
            "Specification-Version" to "0",
            "Specification-Vendor" to "WeaveMC",
            "Implementation-Title" to "Weave Loader API",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "WeaveMC",
        ), "net.weavemc.loader.api"
    )
}

artifacts {
    archives(apiJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(apiJar)

            groupId = "net.weavemc"
            artifactId = "loader"
            version = "${project.version}"
        }
    }
}