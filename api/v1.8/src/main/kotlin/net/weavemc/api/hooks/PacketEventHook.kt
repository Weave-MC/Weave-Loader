@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.bytecode.*
import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.event.PacketEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal class PacketEventHook: Hook(getMappedClass("net/minecraft/network/NetworkManager")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val sendPacket = getMappedMethod(
            "net/minecraft/network/NetworkManager",
            "sendPacket",
            "(Lnet/minecraft/network/Packet;)V"
        )

        node.methods.filter { it.name == sendPacket.runtimeName && it.desc.contains(getMappedClass("net/minecraft/network/Packet")) }.forEach {
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
        )

        node.methods.search(channelRead0.runtimeName, channelRead0.desc).instructions.insert(asm {
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
