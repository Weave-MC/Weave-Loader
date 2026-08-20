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
            "CommandEventRegisterHook",
            "EntityListEventHook",
            "GuiOpenEventHook",
            "KeyboardEventHook",
            "MouseEventHook",
            "PacketEventHook",
            "RenderGameOverlayEventHook",
            "RenderHandEventHook",
            "RenderLivingEventHook",
            "RenderWorldEventHook",
            "ServerConnectionEventConnectHook",
            "ServerConnectionEventDisconnectHook",
            "ShutdownEventHook",
            "StartGameEventHook",
            "TickEventHook",
            "WorldEventHook",
        ).map { "net.weavemc.api.hook.$it" }
        accessWideners = listOf("net.weave.api.v1_21.accesswidener.txt")
        yarnMappings()
    }
    version("1.21.11")
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.bundles.asm)
    implementation(libs.weave.internals)
    implementation(projects.api)
}

kotlin {
    jvmToolchain(21)
}