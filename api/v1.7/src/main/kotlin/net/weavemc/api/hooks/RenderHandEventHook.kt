@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.*
import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.event.RenderHandEvent
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode

/**
 * @see net.minecraft.client.renderer.EntityRenderer.renderWorld
 */
class RenderHandEventHook : Hook("net/minecraft/client/renderer/EntityRenderer") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val renderWorld = node.methods.named("renderWorld")

        val ifeq = renderWorld.instructions.find {
            it is LdcInsnNode && it.cst == "hand"
        }!!.next<JumpInsnNode> { it.opcode == Opcodes.IFEQ }!!

        renderWorld.instructions.insert(
            ifeq,
            asm {
                new(internalNameOf<RenderHandEvent>())
                dup; dup
                fload(1)
                invokespecial(internalNameOf<RenderHandEvent>(), "<init>", "(F)V")

                callEvent()

                invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
                ifne(ifeq.label)
            }
        )
    }
}
