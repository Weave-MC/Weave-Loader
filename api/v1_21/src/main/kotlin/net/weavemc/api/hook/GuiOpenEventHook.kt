package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.GuiOpenEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

internal class GuiOpenEventHook : Hook("net/minecraft/client/MinecraftClient") {
    /**
     * @see net.minecraft.client.MinecraftClient.setScreen
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("setScreen").instructions.insert(asm {
            new(internalNameOf<GuiOpenEvent>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<GuiOpenEvent>(),
                "<init>",
                "(Lnet/minecraft/client/gui/screen/Screen;)V"
            )
            dup
            postEvent()

            returnIfEventCancelled()
        })
    }
}
