package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.ChatEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

internal class ChatEventReceivedHook : Hook("net/minecraft/client/gui/GuiNewChat") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("printChatMessageWithOptionalDeletion").instructions.insert(asm {
            new(internalNameOf<ChatEvent.Received>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<ChatEvent.Received>(),
                "<init>",
                "(Lnet/minecraft/util/text/ITextComponent;)V"
            )
            dup
            postEvent()
            dup

            returnIfEventCancelled(asm {
                pop
                _return
            })

            invokevirtual(
                internalNameOf<ChatEvent.Received>(),
                "getMessage",
                "()Lnet/minecraft/text/Text;"
            )
            astore(1)
        })

        cfg.computeFrames()
    }
}
