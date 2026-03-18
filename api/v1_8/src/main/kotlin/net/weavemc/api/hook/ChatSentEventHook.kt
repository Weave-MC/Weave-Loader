package net.weavemc.api.hook

import net.weavemc.api.ChatSentEvent
import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.CancellableEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal object ChatSentEventHook : Hook("net/minecraft/client/entity/EntityPlayerSP") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("sendChatMessage").instructions.insert(asm {
            new(internalNameOf<ChatSentEvent>())
            dup
            dup
            aload(1)
            invokespecial(
                internalNameOf<ChatSentEvent>(),
                "<init>",
                "(L${internalNameOf<String>()};)V"
            )
            postEvent()

            val end = LabelNode()

            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifeq(end)

            _return

            +end
            f_same()
        })
    }
}
