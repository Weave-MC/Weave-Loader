@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.api.event.EntityListEvent
import org.objectweb.asm.Opcodes
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
            callEvent()
        })
    }
}

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
            callEvent()
        })
    }
}
