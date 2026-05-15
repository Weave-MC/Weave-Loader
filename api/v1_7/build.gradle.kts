plugins {
    id("config-kotlin")
    id("config-publish")
    id("net.weavemc.gradle")
}

group = "net.weavemc.api"
version = libs.versions.weave.get()

weave {
    configure {
        name = "Weave-API"
        modId = "net.weavemc.api.${project.name}"
        hooks = listOf(
            "ChatEventReceivedHook",
            "ChatEventSentHook",
            "ClientConnectedToServerEventHook",
            "EntityListEventAddHook",
            "EntityListEventRemoveHook",
            "GuiOpenEventHook",
            "KeyboardEventHook",
            "MouseEventHook",
            "PacketEventHook",
            "PlayerListEventHook",
            "RenderGameOverlayHook",
            "RenderHandEventHook",
            "RenderLivingEventHook",
            "RenderWorldEventHook",
            "ShutdownEventHook",
            "StartGameEventHook",
            "TickEventHook",
            "WorldEventHook",
        ).map { "net.weavemc.api.hook.$it" }
        accessWideners = listOf("net.weave.api.v1_7.accesswidener.txt")
        mcpMappings()
    }
    version("1.7.10")
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.weave.internals)
    implementation(projects.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "net.weavemc.api"
            artifactId = "api-${project.name}"
            this.version = version
        }
    }
}