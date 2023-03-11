package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.api.event.TickEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named

fun HookManager.registerTickHook() = register("net/minecraft/client/Minecraft") {
    node.methods.named("runTick").instructions.insert(asm {
        getSingleton<EventBus>()
        getSingleton<TickEvent>()
        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    })
}
