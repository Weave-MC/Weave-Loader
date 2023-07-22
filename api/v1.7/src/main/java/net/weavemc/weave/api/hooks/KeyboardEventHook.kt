@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import KeyboardEvent
import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

internal class KeyboardEventHook : Hook("net/minecraft/client/Minecraft") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("runTick").let { mn ->
            mn.instructions.insert(
                mn.instructions.find { it is MethodInsnNode && it.name == "dispatchKeypresses" },
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
