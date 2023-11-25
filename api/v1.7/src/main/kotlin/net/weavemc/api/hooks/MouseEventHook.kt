@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.event.MouseEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
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
        )

        val mn = node.methods.search(runTick.runtimeName, runTick.desc)

        val mouseNext = mn.instructions.find {
            it is MethodInsnNode && it.owner == internalNameOf<Mouse>() && it.name == "next"
        }!!

        val top = LabelNode()
        mn.instructions.insertBefore(mouseNext, top)
        mn.instructions.insert(mouseNext.next, asm {
            new(internalNameOf<net.weavemc.api.event.MouseEvent>())
            dup; dup
            invokespecial(internalNameOf<net.weavemc.api.event.MouseEvent>(), "<init>", "()V")
            callEvent()

            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifne(top)
        })
    }
}
