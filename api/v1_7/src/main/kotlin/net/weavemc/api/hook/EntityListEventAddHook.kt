package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.EntityListEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

internal class EntityListEventAddHook : Hook("net/minecraft/world/World") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("spawnEntityInWorld").instructions.insert(asm {
            new(internalNameOf<EntityListEvent.Add>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<EntityListEvent.Add>(),
                "<init>",
                "(Lnet/minecraft/entity/Entity;)V"
            )
            postEvent()
        })
    }
}