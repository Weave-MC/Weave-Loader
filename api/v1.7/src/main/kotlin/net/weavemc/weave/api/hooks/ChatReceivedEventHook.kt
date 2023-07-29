@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.ChatReceivedEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * @see net.minecraft.client.gui.GuiNewChat.printChatMessageWithOptionalDeletion
 */
class ChatReceivedEventHook : Hook(getMappedClass("net/minecraft/client/gui/GuiNewChat")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/gui/GuiNewChat",
            "printChatMessageWithOptionalDeletion",
            "(Lnet/minecraft/util/IChatComponent;I)V"
        ) ?: error("Failed to find mapping for GuiNewChat#printChatMessageWithOptionalDeletion")

        node.methods.search(mappedMethod.name, mappedMethod.descriptor).instructions.insert(asm {
            new(internalNameOf<ChatReceivedEvent>())
            dup
            dup
            aload(1)
            invokespecial(
                internalNameOf<ChatReceivedEvent>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/util/IChatComponent")};)V"
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
