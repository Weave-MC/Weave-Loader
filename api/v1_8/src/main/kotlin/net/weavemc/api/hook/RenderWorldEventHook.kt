package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.RenderWorldEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

/**
 * Corresponds to [RenderWorldEvent].
 */
internal class RenderWorldEventHook : Hook("net/minecraft/client/renderer/EntityRenderer") {
    /**
     * Inserts a call to [RenderWorldEvent]'s constructor at the head of
     * [net.minecraft.client.renderer.EntityRenderer.renderWorldPass], which
     * is called in the event of any world render.
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("renderWorldPass")

        mn.instructions.insertBefore(
            mn.instructions.find { it is LdcInsnNode && it.cst == "hand" },
            asm {
                new(internalNameOf<RenderWorldEvent>())
                dup
                fload(2)
                invokespecial(internalNameOf<RenderWorldEvent>(), "<init>", "(F)V")
                postEvent()
            }
        )
    }
}
