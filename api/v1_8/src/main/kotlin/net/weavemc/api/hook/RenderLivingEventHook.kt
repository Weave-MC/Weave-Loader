package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.RenderLivingEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.CancellableEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * Corresponds to [RenderLivingEvent.Pre] and [RenderLivingEvent.Post].
 */
internal object RenderLivingEventHook : Hook("net/minecraft/client/renderer/entity/RendererLivingEntity") {

    /**
     * Inserts a call to [RenderLivingEvent.Pre]'s constructor at the head of
     * [net.minecraft.client.renderer.entity.RendererLivingEntity.doRender], which
     * is called in the event of any entity render.
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("doRender")
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
                "(Lnet/minecraft/client/renderer/entity/RendererLivingEntity;Lnet/minecraft/entity/EntityLivingBase;DDDF)V"
            )
            postEvent()

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
                "(Lnet/minecraft/client/renderer/entity/RendererLivingEntity;Lnet/minecraft/entity/EntityLivingBase;DDDF)V"
            )
            postEvent()
        })
    }
}
