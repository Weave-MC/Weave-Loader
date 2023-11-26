@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.api.event.KeyboardEvent
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @see net.minecraft.client.Minecraft.runTick
 */
class KeyboardEventHook : Hook("net/minecraft/client/Minecraft") {
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
