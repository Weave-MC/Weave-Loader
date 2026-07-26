package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.PacketEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import net.weavemc.internals.search
import org.objectweb.asm.tree.ClassNode

internal class PacketEventHook : Hook("net/minecraft/network/ClientConnection") {
    /**
     * @see net.minecraft.network.ClientConnection.send
     * @see net.minecraft.network.ClientConnection.channelRead0
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val methods = node.methods

        methods.search(
            "send",
            "(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"
        ).instructions.insert(generateInstructions(internalNameOf<PacketEvent.Send>(), 1))
        methods.named("channelRead0").instructions.insert(generateInstructions(internalNameOf<PacketEvent.Receive>(), 2))

        cfg.computeFrames()
    }

    private fun generateInstructions(eventName: String, varIndex: Int) = asm {
        new(eventName)
        dup
        aload(varIndex)
        invokespecial(
            eventName,
            "<init>",
            "(Lnet/minecraft/network/packet/Packet;)V"
        )
        dup
        postEvent()
        dup

        returnIfEventCancelled(asm {
            pop
            _return
        })

        invokevirtual(
            eventName,
            "getPacket",
            "()Lnet/minecraft/network/packet/Packet;"
        )
        astore(varIndex)
    }
}
