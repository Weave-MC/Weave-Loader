@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.MouseEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import org.lwjgl.input.Mouse
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @see net.minecraft.client.Minecraft.runTick
 */
class MouseEventHook : Hook(getMappedClass("net/minecraft/client/Minecraft")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val runTick = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "runTick",
            "()V"
        ) ?: error("Failed to find mapping for Minecraft#runTick")

        val mn = node.methods.search(runTick.name, runTick.descriptor)

        val mouseNext = mn.instructions.find {
            it is MethodInsnNode && it.owner == internalNameOf<Mouse>() && it.name == "next"
        }!!

        val top = LabelNode()
        mn.instructions.insertBefore(mouseNext, top)
        mn.instructions.insert(mouseNext.next, asm {
            new(internalNameOf<MouseEvent>())
            dup; dup
            invokespecial(internalNameOf<MouseEvent>(), "<init>", "()V")
            callEvent()

            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifne(top)
        })
    }
}
