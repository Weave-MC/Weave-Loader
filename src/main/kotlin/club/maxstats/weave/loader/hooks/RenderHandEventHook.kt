package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.CancellableEvent
import club.maxstats.weave.loader.api.event.RenderHandEvent
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.LdcInsnNode

fun HookManager.registerRenderHandHook() = register("net/minecraft/client/renderer/EntityRenderer") {
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
