package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.RenderWorldEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import net.weavemc.internals.prev
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * Corresponds to [RenderWorldEvent].
 */
internal class RenderWorldEventHook : Hook("net/minecraft/client/renderer/EntityRenderer") {
    /**
     * Inserts a call to [RenderWorldEvent]'s constructor at the head of
     * [net.minecraft.client.renderer.EntityRenderer.renderWorld], which
     * is called in the event of any world render.
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("renderWorld")

        mn.instructions.insertBefore(
            mn.instructions
                .find { it is LdcInsnNode && it.cst == "clear" }!!
                .prev<VarInsnNode> { it.opcode == Opcodes.ALOAD && it.`var` == 0 },
            asm {
                new(internalNameOf<RenderWorldEvent>())
                dup
                fload(1)
                invokespecial(internalNameOf<RenderWorldEvent>(), "<init>", "(F)V")
                postEvent()
            }
        )
    }
}
