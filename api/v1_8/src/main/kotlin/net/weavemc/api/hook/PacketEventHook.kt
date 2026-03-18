package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.PacketEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.CancellableEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal object PacketEventHook : Hook("net/minecraft/network/NetworkManager") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.filter { it.name == "sendPacket" }.forEach {
            it.instructions.insert(asm {
                new(internalNameOf<PacketEvent.Send>())
                dup; dup
                aload(1)
                invokespecial(internalNameOf<PacketEvent.Send>(), "<init>", "(Lnet/minecraft/network/Packet;)V")
                postEvent()

                val end = LabelNode()
                dup
                invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
                ifeq(end)
                pop
                _return
                +end
                invokevirtual(internalNameOf<PacketEvent.Send>(), "getPacket", "()Lnet/minecraft/network/Packet;")
                astore(1)
                f_same()
            })
        }

        node.methods.named("channelRead0").instructions.insert(asm {
            new(internalNameOf<PacketEvent.Receive>())
            dup; dup
            aload(2)
            invokespecial(internalNameOf<PacketEvent.Receive>(), "<init>", "(Lnet/minecraft/network/Packet;)V")
            postEvent()

            val end = LabelNode()
            dup
            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifeq(end)
            pop
            _return
            +end
            invokevirtual(internalNameOf<PacketEvent.Receive>(), "getPacket", "()Lnet/minecraft/network/Packet;")
            astore(2)
            f_same()
        })

        cfg.computeFrames()
    }
}
