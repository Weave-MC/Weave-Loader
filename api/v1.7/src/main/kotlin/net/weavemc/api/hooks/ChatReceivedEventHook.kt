@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.event.ChatReceivedEvent
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * @see net.minecraft.client.gui.GuiNewChat.printChatMessageWithOptionalDeletion
 */
class ChatReceivedEventHook : Hook("net/minecraft/client/gui/GuiNewChat") {
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
