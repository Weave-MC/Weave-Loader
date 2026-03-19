plugins {
    id("config-kotlin")
    id("config-publish")
    id("net.weavemc.gradle")
}

weave {
    configure {
        name = "Weave-API"
        modId = "net.weavemc.api"
        hooks = listOf(
            "ChatReceivedEventHook",
            "ChatSentEventHook",
            "EntityListEventAddHook",
            "EntityListEventRemoveHook",
            "GuiOpenEventHook",
            "KeyboardEventHook",
            "MouseEventHook",
            "PlayerListEventHook",
            "RenderGameOverlayHook",
            "RenderHandEventHook",
            "RenderLivingEventHook",
            "RenderWorldEventHook",
            "ServerConnectEventHook",
            "ShutdownEventHook",
            "StartGameEventHook",
            "TickEventHook",
            "WorldEventHook",
            "PacketEventHook",
        ).map { "net.weavemc.api.hook.$it" }
        mcpMappings()
    }
    version("1.8.9")
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.internals)
    implementation(libs.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "net.weavemc.api"
            artifactId = "api-${project.name}"
            version = libs.versions.api.get().toString()
        }
    }
}