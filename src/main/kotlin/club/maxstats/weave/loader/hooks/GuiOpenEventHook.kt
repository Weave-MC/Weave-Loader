package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.CancellableEvent
import club.maxstats.weave.loader.api.event.GuiOpenEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import net.minecraft.client.Minecraft
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

class GuiOpenEventHook : Hook(Minecraft::class) {
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
