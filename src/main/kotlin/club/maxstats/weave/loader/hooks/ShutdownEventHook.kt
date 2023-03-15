package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.ShutdownEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.named

fun HookManager.registerShutdownHook() = register("net/minecraft/client/Minecraft") {
    node.methods.named("shutdownMinecraftApplet").instructions.insert(asm {
        getSingleton<ShutdownEvent>()
        callEvent()
    })
}
