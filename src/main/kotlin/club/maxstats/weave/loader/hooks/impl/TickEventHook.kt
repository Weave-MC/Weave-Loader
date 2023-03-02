package club.maxstats.weave.loader.hooks.impl

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.tree.ClassNode

class TickEventHook : Hook("net/minecraft/client/Minecraft") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("runTick").instructions.insert(asm {
            new("club/maxstats/weave/loader/api/event/TickEvent")
            dup
            invokespecial("club/maxstats/weave/loader/api/event/TickEvent", "<init>", "()V")
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