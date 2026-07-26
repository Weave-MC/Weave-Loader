package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.ChatEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.search
import org.objectweb.asm.tree.ClassNode

internal class ChatEventReceivedHook : Hook("net/minecraft/client/gui/hud/ChatHud") {
    /**
     * @see net.minecraft.client.gui.hud.ChatHud.addMessage
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.search(
            "addMessage",
            "(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"
        ).instructions.insert(asm {
            new(internalNameOf<ChatEvent.Received>())
            dup
            aload(1)
            aload(2)
            aload(3)
            invokespecial(
                internalNameOf<ChatEvent.Received>(),
                "<init>",
                "(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"
            )
            dup
            postEvent()
            dup

            returnIfEventCancelled(asm {
                pop
                _return
            })

            dup
            invokevirtual(
                internalNameOf<ChatEvent.Received>(),
                "getMessage",
                "()Lnet/minecraft/text/Text;"
            )
            astore(1)
            invokevirtual(
                internalNameOf<ChatEvent.Received>(),
                "getIndicator",
                "()Lnet/minecraft/client/gui/hud/MessageIndicator;"
            )
            astore(3)
        })

        cfg.computeFrames()
    }
}