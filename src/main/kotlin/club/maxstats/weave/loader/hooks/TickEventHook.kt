package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.TickEvent
import club.maxstats.weave.loader.util.*

fun HookManager.registerTickHook() = register("net/minecraft/client/Minecraft") {
    node.methods.named("runTick").instructions.insert(asm {
        getSingleton<TickEvent>()
        callEvent()
    })
}
