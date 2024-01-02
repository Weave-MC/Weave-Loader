@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.internals.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.event.GuiOpenEvent
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal class GuiOpenEventHook : Hook("net/minecraft/client/Minecraft") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("displayGuiScreen").instructions.insert(asm {
            new(internalNameOf<GuiOpenEvent>())
            dup
            dup
            aload(1)
            invokespecial(
                internalNameOf<GuiOpenEvent>(),
                "<init>",
                "(Lnet/minecraft/client/gui/GuiScreen;)V"
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
