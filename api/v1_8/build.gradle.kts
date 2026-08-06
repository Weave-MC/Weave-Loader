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
            "RenderGameOverlayEventHook",
            "RenderHandEventHook",
            "RenderLivingEventHook",
            "RenderWorldEventHook",
            "ShutdownEventHook",
            "StartGameEventHook",
            "TickEventHook",
            "WorldEventHook",
        ).map { "net.weavemc.api.hook.$it" }
        accessWideners = listOf("net.weave.api.v1_8.accesswidener.txt")
        mcpMappings()
    }
    version("1.8.9")
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.weave.internals)
    implementation(projects.api)
}