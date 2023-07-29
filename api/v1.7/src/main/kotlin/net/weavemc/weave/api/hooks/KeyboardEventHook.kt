@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.KeyboardEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
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
        ) ?: error("Failed to find mapping for Minecraft#runTick")

        val dispatchKeypresses = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "dispatchKeypresses",
            "()V"
        ) ?: error("Failed to find mapping for Minecraft#dispatchKeypresses")


        node.methods.search(runTick.name, runTick.descriptor).let { mn ->
            mn.instructions.insert(
                mn.instructions.find { it is MethodInsnNode && it.name == dispatchKeypresses.name && it.desc == dispatchKeypresses.descriptor },
                asm {
                    new(internalNameOf<KeyboardEvent>())
                    dup
                    invokespecial(
                        internalNameOf<KeyboardEvent>(),
                        "<init>",
                        "()V"
                    )
                    callEvent()
                }
            )
        }
    }
}
