@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.KeyboardEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @see net.minecraft.client.Minecraft.runTick
 */
class KeyboardEventHook : Hook(getMappedClass("net/minecraft/client/Minecraft")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val runTick = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "runTick",
            "()V"
        )

        val dispatchKeypresses = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "dispatchKeypresses",
            "()V"
        )

        node.methods.search(runTick.runtimeName, runTick.desc).let { mn ->
            mn.instructions.insert(
                mn.instructions.find { it is MethodInsnNode && it.name == dispatchKeypresses.runtimeName && it.desc == dispatchKeypresses.desc },
                asm {
                    new(internalNameOf<net.weavemc.api.event.KeyboardEvent>())
                    dup
                    invokespecial(
                        internalNameOf<net.weavemc.api.event.KeyboardEvent>(),
                        "<init>",
                        "()V"
                    )
                    callEvent()
                }
            )
        }
    }
}
