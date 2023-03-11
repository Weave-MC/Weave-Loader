package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.api.event.ShutdownEvent
import club.maxstats.weave.loader.util.*

fun HookManager.registerShutdownHook() = register("net/minecraft/client/Minecraft") {
    node.methods.named("shutdownMinecraftApplet").instructions.insert(asm {
        getSingleton<EventBus>()
        getSingleton<ShutdownEvent>()
        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    })
}
