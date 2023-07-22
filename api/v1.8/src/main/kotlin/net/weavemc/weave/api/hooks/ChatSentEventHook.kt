@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.ChatSentEvent
import net.weavemc.weave.api.not
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * @see net.minecraft.client.entity.EntityPlayerSP.sendChatMessage
 */
class ChatSentEventHook : Hook(!"net/minecraft/client/entity/EntityPlayerSP") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.search(!"sendChatMessage", "V", "Ljava/lang/String;").instructions.insert(asm {
            new(internalNameOf<ChatSentEvent>())
            dup
            dup
            aload(1)
            invokespecial(
                internalNameOf<ChatSentEvent>(),
                "<init>",
                "(L${internalNameOf<String>()};)V"
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
