@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.*
import net.weavemc.weave.api.event.PlayerListEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

internal class PlayerListEventHook : Hook(getMappedClass("net/minecraft/client/network/NetHandlerPlayClient")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val addInsn = asm {
            new(internalNameOf<PlayerListEvent.Add>())
            dup
            aload(3)
            invokespecial(
                internalNameOf<PlayerListEvent.Add>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/network/play/server/S38PacketPlayerListItem\$AddPlayerData")};)V"
            )
            callEvent()
        }

        val removeInsn = asm {
            new(internalNameOf<PlayerListEvent.Remove>())
            dup
            aload(3)
            invokespecial(
                internalNameOf<PlayerListEvent.Remove>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/network/play/server/S38PacketPlayerListItem\$AddPlayerData")};)V"
            )
            callEvent()
        }

        val mappedMethod = getMappedMethod(
            "net/minecraft/client/network/NetHandlerPlayClient",
            "handlePlayerListItem",
            "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem;)V"
        )

        val mn = node.methods.search(mappedMethod.runtimeName, mappedMethod.descriptor)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "put" }, addInsn)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "remove" }, removeInsn)
    }
}
