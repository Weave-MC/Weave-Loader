@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.bytecode.*
import net.weavemc.api.event.PlayerListEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
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

        val mn = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "put" }, addInsn)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "remove" }, removeInsn)
    }
}
