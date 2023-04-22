package net.weavemc.loader.hooks.client

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.client.LivingUpdateEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal class LivingUpdateEventHook : Hook("net/minecraft/entity/EntityLivingBase") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("onUpdate").instructions.insert(asm {
            new(internalNameOf<LivingUpdateEvent>())
            dup
            dup
            aload(0)
            invokespecial(internalNameOf<LivingUpdateEvent>(), "<init>", "(Lnet/minecraft/entity/EntityLivingBase;)V")
            callEvent()

            val end = LabelNode()

            invokevirtual(internalNameOf<LivingUpdateEvent>(), "isCancelled", "()Z")
            ifeq(end)

            _return

            +end
            f_same()
        })
    }
}
