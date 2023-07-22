@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.PlayerListEvent
import net.weavemc.weave.api.not
import net.weavemc.weave.api.unaryMinus
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @see net.minecraft.client.network.NetHandlerPlayClient.handlePlayerListItem
 */
class PlayerListEventHook : Hook(!"net/minecraft/client/network/NetHandlerPlayClient") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val addInsn = asm {
            new(internalNameOf<PlayerListEvent.Add>())
            dup
            aload(2)
            invokespecial(
                internalNameOf<PlayerListEvent.Add>(),
                "<init>",
                -"(Lnet/minecraft/client/gui/GuiPlayerInfo;)V"
            )
            callEvent()
        }

        val removeInsn = asm {
            new(internalNameOf<PlayerListEvent.Remove>())
            dup
            aload(2)
            invokespecial(
                internalNameOf<PlayerListEvent.Remove>(),
                "<init>",
                -"(Lnet/minecraft/client/gui/GuiPlayerInfo;)V"
            )
            callEvent()
        }

        val mn = node.methods.search(!"handlePlayerListItem", "V", -"Lnet/minecraft/network/play/server/S38PacketPlayerListItem;")
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "put" }, addInsn)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "remove" }, removeInsn)
    }
}
