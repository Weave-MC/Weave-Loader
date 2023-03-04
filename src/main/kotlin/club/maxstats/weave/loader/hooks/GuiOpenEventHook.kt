package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named

fun HookManager.registerGuiOpenHook() = register("net/minecraft/client/Minecraft") {
    node.methods.named("displayGuiScreen").instructions.insert(asm {
        getSingleton<EventBus>()

        new("club/maxstats/weave/loader/api/event/GuiOpenEvent")
        dup
        aload(1)
        invokespecial(
            "club/maxstats/weave/loader/api/event/GuiOpenEvent",
            "<init>",
            "(Lnet/minecraft/client/gui/GuiScreen;)V"
        )

        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    })
}