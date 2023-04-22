package net.weavemc.loader.hooks.client

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.client.PlayerListEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

internal class PlayerListEventHook : Hook("net/minecraft/client/network/NetHandlerPlayClient") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val addInsn = asm {
            new(internalNameOf<PlayerListEvent.Add>())
            dup
            aload(3)
            invokespecial(
                internalNameOf<PlayerListEvent.Add>(),
                "<init>",
                "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem\$AddPlayerData;)V"
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
                "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem\$AddPlayerData;)V"
            )
            callEvent()
        }

        val mn = node.methods.named("handlePlayerListItem")
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "put" }, addInsn)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "remove" }, removeInsn)
    }
}
