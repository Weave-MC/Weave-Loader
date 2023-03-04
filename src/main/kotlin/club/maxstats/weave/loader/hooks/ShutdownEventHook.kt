package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.api.event.ShutdownEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named

fun HookManager.registerShutdownHook() = register("net/minecraft/client/Minecraft") {
    node.methods.named("shutdownMinecraftApplet").instructions.insert(asm {
        getSingleton<EventBus>()
        new(internalNameOf<ShutdownEvent>())
        dup
        invokespecial(internalNameOf<ShutdownEvent>(), "<init>", "()V")
        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    })
}