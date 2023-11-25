@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import api.bytecode.*
import net.weavemc.api.bytecode.*
import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.bytecode.*
import net.weavemc.api.event.RenderHandEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode

/**
 * @see net.minecraft.client.renderer.EntityRenderer.renderWorld
 */
class RenderHandEventHook : Hook(getMappedClass("net/minecraft/client/renderer/EntityRenderer")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/renderer/EntityRenderer",
            "renderWorld",
            "(FJ)V"
        )

        val renderWorld = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)

        val ifeq = renderWorld.instructions.find {
            it is LdcInsnNode && it.cst == "hand"
        }!!.next<JumpInsnNode> { it.opcode == Opcodes.IFEQ }!!

        renderWorld.instructions.insert(
            ifeq,
            asm {
                new(internalNameOf<net.weavemc.api.event.RenderHandEvent>())
                dup; dup
                fload(1)
                invokespecial(internalNameOf<net.weavemc.api.event.RenderHandEvent>(), "<init>", "(F)V")

                callEvent()

                invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
                ifne(ifeq.label)
            }
        )
    }
}
