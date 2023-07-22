@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.PacketEvent
import net.weavemc.weave.api.not
import net.weavemc.weave.api.unaryMinus
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * @see net.minecraft.network.NetworkManager.scheduleOutboundPacket
 * @see net.minecraft.network.NetworkManager.channelRead0
 */
class PacketEventHook: Hook(!"net/minecraft/network/NetworkManager") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {

        node.methods.search(!"scheduleOutboundPacket", "V", -"Lnet/minecraft/network/Packet;", "[Lio/netty/util/concurrent/GenericFutureListener;").instructions.insert(asm {
            new(internalNameOf<PacketEvent.Send>())
            dup; dup
            aload(1)
            invokespecial(internalNameOf<PacketEvent.Send>(), "<init>", -"(Lnet/minecraft/network/Packet;)V")
            callEvent()

            val end = LabelNode()
            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifeq(end)
            _return
            +end
            f_same()
        })


        node.methods.search(!"channelRead0", "V", "Lio/netty/channel/ChannelHandlerContext;", -"Lnet/minecraft/network/Packet;").instructions.insert(asm {
            new(internalNameOf<PacketEvent.Receive>())
            dup; dup
            aload(2)
            invokespecial(internalNameOf<PacketEvent.Receive>(), "<init>", -"(Lnet/minecraft/network/Packet;)V")
            callEvent()

            val end = LabelNode()
            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifeq(end)
            _return
            +end
            f_same()
        })
    }
}
