package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.RenderWorldEvent
import club.maxstats.weave.loader.api.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

internal class RenderWorldEventHook : Hook("net/minecraft/client/renderer/EntityRenderer") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("renderWorldPass")

        mn.instructions.insertBefore(
            mn.instructions.find { it is LdcInsnNode && it.cst == "hand" },
            asm {
                new(internalNameOf<RenderWorldEvent>())
                dup
                fload(2)
                invokespecial(internalNameOf<RenderWorldEvent>(), "<init>", "(F)V")
                callEvent()
            }
        )
    }
}
