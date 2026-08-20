package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.EntityListEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

internal class EntityListEventRemoveHook : Hook("net/minecraft/client/multiplayer/WorldClient") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("removeEntityFromWorld")
        mn.instructions.insert(mn.instructions.find { it.opcode == Opcodes.IFNULL }, asm {
            new(internalNameOf<EntityListEvent.Remove>())
            dup
            aload(2)
            invokespecial(
                internalNameOf<EntityListEvent.Remove>(),
                "<init>",
                "(Lnet/minecraft/entity/Entity;)V"
            )
            postEvent()
        })
    }
}
