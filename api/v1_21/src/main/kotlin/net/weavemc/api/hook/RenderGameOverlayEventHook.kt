package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.RenderGameOverlayEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

internal class RenderGameOverlayEventHook : Hook("net/minecraft/client/gui/hud/InGameHud") {
    /**
     * @see net.minecraft.client.gui.hud.InGameHud.render
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val instructions = node.methods.named("render").instructions

        instructions.insert(generateInstructions(internalNameOf<RenderGameOverlayEvent.Pre>()))
        instructions.insertBefore(instructions.last { it.opcode == Opcodes.RETURN }, generateInstructions(internalNameOf<RenderGameOverlayEvent.Post>()))

        cfg.computeFrames()
    }

    private fun generateInstructions(eventName: String) = asm {
        new(eventName)
        dup
        aload(1)
        aload(2)
        invokespecial(
            eventName,
            "<init>",
            "(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
        )
        postEvent()
    }
}