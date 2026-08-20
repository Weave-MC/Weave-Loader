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
 * Corresponds to [ServerConnectionEvent.Disconnect].
 */
internal class ServerConnectionEventDisconnectHook : Hook("net/minecraft/client/network/ClientCommonNetworkHandler") {
    /**
     * Inserts a call to [ServerConnectionEvent.Disconnect] at the tail of [net.minecraft.client.network.ClientCommonNetworkHandler.onDisconnected].
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("onDisconnected")

        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == Opcodes.RETURN }, asm {
            new(internalNameOf<ServerConnectionEvent.Disconnect>())
            dup
            aload(0)
            getfield(
                "net/minecraft/client/network/ClientCommonNetworkHandler",
                "connection",
                "Lnet/minecraft/network/ClientConnection;"
            )
            aload(1)
            invokespecial(
                internalNameOf<ServerConnectionEvent.Disconnect>(),
                "<init>",
                "(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/network/DisconnectionInfo;)V"
            )
            postEvent()
        })
    }}