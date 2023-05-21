package net.weavemc.loader.hooks

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.RenderGameOverlayEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

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
            callEvent()
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
            callEvent()
        })
    }
}
