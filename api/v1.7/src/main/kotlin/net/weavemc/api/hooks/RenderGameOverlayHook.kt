@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.RenderGameOverlayEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

/**
 * @see net.minecraft.client.gui.GuiIngame.renderGameOverlay
 */
class RenderGameOverlayHook : Hook(
    getMappedClass("net/minecraft/client/gui/GuiIngame"),
    "net/minecraftforge/client/GuiIngameForge"
) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/gui/GuiIngame",
            "renderGameOverlay",
            "(FZII)V"
        )

        val mn = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)

        mn.instructions.insert(asm {
            new(internalNameOf<net.weavemc.api.event.RenderGameOverlayEvent.Pre>())
            dup
            fload(1)
            invokespecial(
                internalNameOf<net.weavemc.api.event.RenderGameOverlayEvent.Pre>(),
                "<init>",
                "(F)V"
            )
            callEvent()
        })

        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == RETURN }, asm {
            new(internalNameOf<net.weavemc.api.event.RenderGameOverlayEvent.Post>())
            dup
            fload(1)
            invokespecial(
                internalNameOf<net.weavemc.api.event.RenderGameOverlayEvent.Post>(),
                "<init>",
                "(F)V"
            )
            callEvent()
        })
    }
}
