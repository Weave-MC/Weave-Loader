@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.*
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.PacketEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal class PacketEventHook: Hook(getMappedClass("net/minecraft/network/NetworkManager")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val sendPacket = getMappedMethod(
            "net/minecraft/network/NetworkManager",
            "sendPacket",
            "(Lnet/minecraft/network/Packet;)V"
        ) ?: error("Failed to find mapping for sendPacket")

        node.methods.filter { it.name == sendPacket.name && it.desc.contains(getMappedClass("net/minecraft/network/Packet")!!) }.forEach {
            it.instructions.insert(asm {
                new(internalNameOf<PacketEvent.Send>())
                dup; dup
                aload(1)
                invokespecial(internalNameOf<PacketEvent.Send>(), "<init>", "(L${getMappedClass("net/minecraft/network/Packet")};)V")
                callEvent()

                val end = LabelNode()
                invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
                ifeq(end)
                _return
                +end
                f_same()
            })
        }

        val channelRead0 = getMappedMethod(
            "net/minecraft/network/NetworkManager",
            "channelRead0",
            "(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V"
        ) ?: error("Failed to find mapping for channelRead0")

        node.methods.search(channelRead0.name, channelRead0.descriptor).instructions.insert(asm {
            new(internalNameOf<PacketEvent.Receive>())
            dup; dup
            aload(2)
            invokespecial(internalNameOf<PacketEvent.Receive>(), "<init>", "(L${getMappedClass("net/minecraft/network/Packet")};)V")
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
