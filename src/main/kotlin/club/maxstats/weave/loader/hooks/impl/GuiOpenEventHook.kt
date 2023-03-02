package club.maxstats.weave.loader.hooks.impl

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.tree.ClassNode

class GuiOpenEventHook : Hook("net/minecraft/client/Minecraft") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("displayGuiScreen").instructions.insert(asm {
            new("club/maxstats/weave/loader/api/event/GuiOpenEvent")
            dup
            aload(1)
            invokespecial("club/maxstats/weave/loader/api/event/GuiOpenEvent", "<init>", "(Lnet/minecraft/client/gui/GuiScreen;)V")
            getSingleton<EventBus>()
            swap
            invokevirtual(
                internalNameOf<EventBus>(),
                "callEvent",
                "(L${internalNameOf<Event>()};)V"
            )
        })
    }
}