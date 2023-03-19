package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.api.event.RenderWorldEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

class RenderWorldEventHook : Hook("net/minecraft/client/renderer/EntityRenderer") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("renderWorldPass").let { mn ->
            mn.instructions.insertBefore(
                mn.instructions.find { it is LdcInsnNode && it.cst == "hand" },
                asm {
                    getSingleton<EventBus>()
                    new(internalNameOf<RenderWorldEvent>())
                    dup
                    fload(2)
                    invokespecial(internalNameOf<RenderWorldEvent>(), "<init>", "(F)V")
                    invokevirtual(
                        internalNameOf<EventBus>(),
                        "callEvent",
                        "(L${internalNameOf<Event>()};)V"
                    )
                }
            )
        }
    }
}
