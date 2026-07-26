package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.ServerConnectionEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ServerConnectionEvent.Connect].
 */
internal class ServerConnectionEventConnectHook : Hook("net/minecraft/client/network/ClientLoginNetworkHandler") {
    /**
     * Inserts a call to [ServerConnectionEvent.Connect] at the tail of [net.minecraft.client.network.ClientLoginNetworkHandler.onSuccess].
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val instructions = node.methods.named("onSuccess").instructions

        instructions.insertBefore(instructions.findLast { it.opcode == Opcodes.RETURN }, asm {
            new(internalNameOf<ServerConnectionEvent.Connect>())
            dup
            aload(0)
            getfield(
                "net/minecraft/client/network/ClientLoginNetworkHandler",
                "connection",
                "Lnet/minecraft/network/ClientConnection;"
            )
            invokespecial(
                internalNameOf<ServerConnectionEvent.Connect>(),
                "<init>",
                "(Lnet/minecraft/network/ClientConnection;)V"
            )
            postEvent()
        })
    }
}
