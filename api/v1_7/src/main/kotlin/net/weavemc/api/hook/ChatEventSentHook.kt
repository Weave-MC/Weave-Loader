package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.ChatEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

internal class ChatEventSentHook : Hook("net/minecraft/client/entity/EntityClientPlayerMP") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("sendChatMessage").instructions.insert(asm {
            new(internalNameOf<ChatEvent.Sent>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<ChatEvent.Sent>(),
                "<init>",
                "(L${internalNameOf<String>()};)V"
            )
            dup
            postEvent()
            dup

            returnIfEventCancelled(asm {
                pop
                _return
            })

            invokevirtual(
                internalNameOf<ChatEvent.Sent>(),
                "getMessage",
                "()Ljava/lang/String;"
            )
            astore(1)
        })

        cfg.computeFrames()
    }
}
