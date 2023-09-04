@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.*
import net.weavemc.weave.api.event.CancellableEvent
import net.weavemc.weave.api.event.RenderLivingEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * Corresponds to [RenderLivingEvent.Pre] and [RenderLivingEvent.Post].
 */
internal class RenderLivingEventHook : Hook(getMappedClass("net/minecraft/client/renderer/entity/RendererLivingEntity")) {

    /**
     * Inserts a call to [RenderLivingEvent.Pre]'s constructor at the head of
     * [net.minecraft.client.renderer.entity.RendererLivingEntity.doRender], which
     * is called in the event of any entity render.
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/renderer/entity/RendererLivingEntity",
            "doRender",
            "(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V"
        )

        val mn = node.methods.search(mappedMethod.runtimeName, mappedMethod.descriptor)
        mn.instructions.insert(asm {
            new(internalNameOf<RenderLivingEvent.Pre>())
            dup
            dup
            aload(0)
            aload(1)
            dload(2)
            dload(4)
            dload(6)
            fload(9)
            invokespecial(
                internalNameOf<RenderLivingEvent.Pre>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/client/renderer/entity/RendererLivingEntity")};" +
                    "L${getMappedClass("net/minecraft/entity/EntityLivingBase")};" +
                    "D" +
                    "D" +
                    "D" +
                    "F)V"
            )
            callEvent()

            val end = LabelNode()

            invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
            ifeq(end)

            _return

            +end
            f_same()
        })
        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == Opcodes.RETURN }, asm {
            new(internalNameOf<RenderLivingEvent.Post>())
            dup
            dup
            aload(0)
            aload(1)
            dload(2)
            dload(4)
            dload(6)
            fload(9)
            invokespecial(
                internalNameOf<RenderLivingEvent.Post>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/client/renderer/entity/RendererLivingEntity")};" +
                    "L${getMappedClass("net/minecraft/entity/EntityLivingBase")};" +
                    "D" +
                    "D" +
                    "D" +
                    "F)V"
            )
            callEvent()
        })
    }
}
