package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.MouseEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.CancellableEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.lwjgl.input.Mouse
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode

internal object MouseEventHook : Hook("net/minecraft/client/Minecraft") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("runTick")

        val mouseNext = mn.instructions.find {
            it is MethodInsnNode && it.owner == internalNameOf<Mouse>() && it.name == "next"
        }!!

        val top = LabelNode()
        mn.instructions.insertBefore(mouseNext, top)
        mn.instructions.insert(mouseNext.next, asm {
            new(internalNameOf<MouseEvent>())
            dup; dup
            invokespecial(internalNameOf<MouseEvent>(), "<init>", "()V")
            postEvent()

            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifne(top)
        })
    }
}
