@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.named
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.PacketEvent
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

class PacketEventHook: Hook("net/minecraft/network/NetworkManager") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {

        node.methods.filter { it.name == "scheduleOutboundPacket" }.forEach {
            it.instructions.insert(asm {
                new(internalNameOf<PacketEvent.Send>())
                dup; dup
                aload(1)
                invokespecial(internalNameOf<PacketEvent.Send>(), "<init>", "(Lnet/minecraft/network/Packet;)V")
                callEvent()

                val end = LabelNode()
                invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
                ifeq(end)
                _return
                +end
                f_same()
            })
        }

        node.methods.named("channelRead0").instructions.insert(asm {
            new(internalNameOf<PacketEvent.Receive>())
            dup; dup
            aload(2)
            invokespecial(internalNameOf<PacketEvent.Receive>(), "<init>", "(Lnet/minecraft/network/Packet;)V")
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
