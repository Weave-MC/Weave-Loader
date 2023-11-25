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

internal class RenderHandEventHook : Hook(getMappedClass("net/minecraft/client/renderer/EntityRenderer")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/renderer/EntityRenderer",
            "renderWorldPass",
            "(IFJ)V"
        )

        val renderWorldPass = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)

        val ifeq = renderWorldPass.instructions.find {
            it is LdcInsnNode && it.cst == "hand"
        }!!.next<JumpInsnNode> { it.opcode == Opcodes.IFEQ }!!

        renderWorldPass.instructions.insert(
            ifeq,
            asm {
                new(internalNameOf<RenderHandEvent>())
                dup; dup
                fload(2)
                invokespecial(internalNameOf<RenderHandEvent>(), "<init>", "(F)V")

                callEvent()

                invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
                ifne(ifeq.label)
            }
        )
    }
}
