@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.GuiOpenEvent
import net.weavemc.weave.api.not
import net.weavemc.weave.api.unaryMinus
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * @see net.minecraft.client.Minecraft.displayGuiScreen
 */
class GuiOpenEventHook : Hook(!"net/minecraft/client/Minecraft") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.search(!"displayGuiScreen", "V", -"Lnet/minecraft/client/gui/GuiScreen;").instructions.insert(asm {
            new(internalNameOf<GuiOpenEvent>())
            dup
            dup
            aload(1)
            invokespecial(
                internalNameOf<GuiOpenEvent>(),
                "<init>",
                -"(Lnet/minecraft/client/gui/GuiScreen;)V"
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
