@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import org.objectweb.asm.tree.ClassNode

class ChatSentEventHook : Hook("net/minecraft/client/entity/EntityPlayerSP") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
//        node.methods.named("sendChatMessage").instructions.insert(asm {
//            new(internalNameOf<ChatSentEvent>())
//            dup
//            dup
//            aload(1)
//            invokespecial(
//                internalNameOf<ChatSentEvent>(),
//                "<init>",
//                "(L${internalNameOf<String>()};)V"
//            )
//            callEvent()
//
//            val end = LabelNode()
//
//            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
//            ifeq(end)
//
//            _return
//
//            +end
//            f_same()
//        })
    }
}
