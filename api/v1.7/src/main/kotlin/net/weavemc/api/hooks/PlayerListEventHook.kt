@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.api.event.PlayerListEvent
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

/**
 * @see net.minecraft.client.network.NetHandlerPlayClient.handlePlayerListItem
 */
class PlayerListEventHook : Hook("net/minecraft/client/network/NetHandlerPlayClient") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val addInsn = asm {
            new(internalNameOf<PlayerListEvent.Add>())
            dup
            aload(2)
            invokespecial(
                internalNameOf<PlayerListEvent.Add>(),
                "<init>",
                "(Lnet/minecraft/client/gui/GuiPlayerInfo;)V"
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
                "(Lnet/minecraft/client/gui/GuiPlayerInfo;)V"
            )
            callEvent()
        }

        val mn = node.methods.named("handlePlayerListItem")
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "put" }, addInsn)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "remove" }, removeInsn)
    }
}
