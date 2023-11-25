@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.RenderWorldEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

/**
 * Corresponds to [RenderWorldEvent].
 */
class RenderWorldEventHook : Hook(getMappedClass("net/minecraft/client/renderer/EntityRenderer")) {

    /**
     * Inserts a call to [RenderWorldEvent]'s constructor at the head of
     * [net.minecraft.client.renderer.EntityRenderer.renderWorld], which
     * is called in the event of any world render.
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/renderer/EntityRenderer",
            "renderWorld",
            "(FJ)V"
        )

        val renderWorld = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)

        renderWorld.instructions.insertBefore(
            renderWorld.instructions.find { it is LdcInsnNode && it.cst == "hand" },
            asm {
                new(internalNameOf<net.weavemc.api.event.RenderWorldEvent>())
                dup
                fload(1)
                invokespecial(internalNameOf<net.weavemc.api.event.RenderWorldEvent>(), "<init>", "(F)V")
                callEvent()
            }
        )
    }
}
