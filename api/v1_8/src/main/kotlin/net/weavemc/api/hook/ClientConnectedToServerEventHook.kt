package net.weavemc.api.hook

import net.minecraft.client.network.NetHandlerLoginClient
import net.weavemc.api.Hook
import net.weavemc.api.ClientConnectedToServerEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ClientConnectedToServerEvent].
 */
internal class ClientConnectedToServerEventHook : Hook("net/minecraft/client/network/NetHandlerLoginClient") {
    /**
     * Inserts a call to [ClientConnectedToServerEvent] at the tail of [net.minecraft.client.network.NetHandlerLoginClient.handleLoginSuccess].
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("handleLoginSuccess")
        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == RETURN }, asm {
            new(internalNameOf<ClientConnectedToServerEvent>())
            dup
            aload(0)
            getfield(
                "net/minecraft/client/network/NetHandlerLoginClient",
                "networkManager",
                "Lnet/minecraft/network/NetworkManager;"
            )
            invokespecial(
                internalNameOf<ClientConnectedToServerEvent>(),
                "<init>",
                "(Lnet/minecraft/network/NetworkManager;)V"
            )
            postEvent()
        })
    }
}
