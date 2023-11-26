@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.api.event.RenderGameOverlayEvent
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

internal class RenderGameOverlayHook : Hook("net/minecraft/client/gui/GuiIngame") {
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
