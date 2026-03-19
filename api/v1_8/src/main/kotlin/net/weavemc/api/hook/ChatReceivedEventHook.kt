package net.weavemc.api.hook

import net.weavemc.api.ChatReceivedEvent
import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.CancellableEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal class ChatReceivedEventHook : Hook("net/minecraft/client/gui/GuiNewChat") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("printChatMessageWithOptionalDeletion").instructions.insert(asm {
            new(internalNameOf<ChatReceivedEvent>())
            dup
            dup
            aload(1)
            invokespecial(
                internalNameOf<ChatReceivedEvent>(),
                "<init>",
                "(Lnet/minecraft/util/IChatComponent;)V"
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
