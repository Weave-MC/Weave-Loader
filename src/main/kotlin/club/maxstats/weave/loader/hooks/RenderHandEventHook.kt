package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.CancellableEvent
import club.maxstats.weave.loader.api.event.RenderHandEvent
import club.maxstats.weave.loader.util.*
import net.minecraft.client.renderer.EntityRenderer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

class RenderHandEventHook : Hook(EntityRenderer::class) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val renderWorldPass = node.methods.named("renderWorldPass")

        renderWorldPass.instructions.insertBefore(
            renderWorldPass.instructions.find {
                it is LdcInsnNode && it.cst == "hand"
            }!!.next { it.opcode == Opcodes.IFEQ },

            asm {
                new(internalNameOf<RenderHandEvent>())
                dup; dup
                fload(2)
                invokespecial(internalNameOf<RenderHandEvent>(), "<init>", "(F)V")

                callEvent()

                //this.renderHand & ~event.isCancelled()
                invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
                iconst_1
                ixor
                iand
            }
        )
    }
}
