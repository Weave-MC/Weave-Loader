package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.PlayerListEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

internal class PlayerListEventHook : Hook("net/minecraft/client/network/NetHandlerPlayClient") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val addInsn = asm {
            new(internalNameOf<PlayerListEvent.Add>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<PlayerListEvent.Add>(),
                "<init>",
                "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem;)V"
            )
            postEvent()
        }

        val removeInsn = asm {
            new(internalNameOf<PlayerListEvent.Remove>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<PlayerListEvent.Remove>(),
                "<init>",
                "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem;)V"
            )
            postEvent()
        }

        val mn = node.methods.named("handlePlayerListItem")
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode }, addInsn)
        mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode }, removeInsn)
    }
}
