package net.weavemc.loader.hooks.client

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.client.LivingAttackEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

internal class LivingAttackEventHook : Hook("net/minecraft/entity/EntityLivingBase") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("attackEntityFrom").instructions.insert(asm {
            new(internalNameOf<LivingAttackEvent>())
            dup
            dup
            aload(0)
            aload(1)
            fload(2)
            invokespecial(internalNameOf<LivingAttackEvent>(), "<init>", "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)V")
            callEvent()

            val end = LabelNode()

            invokevirtual(internalNameOf<LivingAttackEvent>(), "isCancelled", "()Z")
            ifeq(end)

            iconst_0
            ireturn

            +end
        })

        cfg.computeFrames()
    }
}
