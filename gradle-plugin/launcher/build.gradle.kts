plugins {
    id("config-kotlin")
    id("config-publish")
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(project(":internals"))
    runtimeOnly(libs.log4j.slf4j2.impl)
    runtimeOnly(libs.log4j.core)
    runtimeOnly(libs.terminalconsoleappender) {
        exclude(group = "org.apache.logging.log4j")
    }

    runtimeOnly(libs.bundles.jline)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "net.weavemc"
            artifactId = "weave-gradle-launcher"
            version = project.version.toString()
        }
    }
}