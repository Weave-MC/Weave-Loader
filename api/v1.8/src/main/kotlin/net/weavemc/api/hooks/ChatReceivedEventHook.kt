@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.bytecode.*
import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.event.ChatReceivedEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal class ChatReceivedEventHook : Hook(getMappedClass("net/minecraft/client/gui/GuiNewChat")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/gui/GuiNewChat",
            "printChatMessageWithOptionalDeletion",
            "(Lnet/minecraft/util/IChatComponent;I)V"
        )

        node.methods.search(mappedMethod.runtimeName, mappedMethod.desc).instructions.insert(asm {
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
