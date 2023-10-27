@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.weave.api.bytecode.*
import net.weavemc.weave.api.event.KeyboardEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

internal class KeyboardEventHook : Hook(getMappedClass("net/minecraft/client/Minecraft")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "runTick",
            "()V"
        )

        node.methods.search(mappedMethod.runtimeName, mappedMethod.desc).let { mn ->
            val dispatchKeypresses = getMappedMethod(
                "net/minecraft/client/Minecraft",
                "dispatchKeypresses",
                "()V"
            )

            mn.instructions.insert(
                mn.instructions.find { it is MethodInsnNode && it.name == dispatchKeypresses.runtimeName },
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
