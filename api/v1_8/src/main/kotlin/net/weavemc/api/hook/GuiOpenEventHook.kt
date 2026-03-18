package net.weavemc.api.hook

import net.weavemc.api.GuiOpenEvent
import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.CancellableEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal object GuiOpenEventHook : Hook("net/minecraft/client/Minecraft") {
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
