package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.RenderGameOverlayEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

internal class RenderGameOverlayHook : Hook(
    "net/minecraft/client/gui/GuiIngame",
    "net/minecraftforge/client/GuiIngameForge"
) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("renderGameOverlay")

        mn.instructions.insert(asm {
            new(internalNameOf<RenderGameOverlayEvent.Pre>())
            dup
            fload(1)
            invokespecial(
                internalNameOf<RenderGameOverlayEvent.Pre>(),
                "<init>",
                "(F)V"
            )
            postEvent()
        })

        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == RETURN }, asm {
            new(internalNameOf<RenderGameOverlayEvent.Post>())
            dup
            fload(1)
            invokespecial(
                internalNameOf<RenderGameOverlayEvent.Post>(),
                "<init>",
                "(F)V"
            )
            postEvent()
        })
    }
}
