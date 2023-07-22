@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.EntityListEvent
import net.weavemc.weave.api.mapper
import net.weavemc.weave.api.not
import net.weavemc.weave.api.unaryMinus
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * @see net.minecraft.world.World.spawnEntityInWorld
 */
class EntityListEventAddHook : Hook(!"net/minecraft/world/World") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.search(mapper.mapMethod(!"net/minecraft/world/World", "spawnEntityInWorld")!!, "Z", -"Lnet/minecraft/entity/Entity;").instructions.insert(asm {
            new(internalNameOf<EntityListEvent.Add>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<EntityListEvent.Add>(),
                "<init>",
                -"(Lnet/minecraft/entity/Entity;)V"
            )
            callEvent()
        })
    }
}

/**
 * @see net.minecraft.client.multiplayer.WorldClient.removeEntityFromWorld
 */
class EntityListEventRemoveHook : Hook(!"net/minecraft/client/multiplayer/WorldClient") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.search(!"removeEntityFromWorld", -"Lnet/minecraft/entity/Entity;", "I")
        mn.instructions.insert(mn.instructions.find { it.opcode == Opcodes.IFNULL }, asm {
            new(internalNameOf<EntityListEvent.Remove>())
            dup
            aload(2)
            invokespecial(
                internalNameOf<EntityListEvent.Remove>(),
                "<init>",
                -"(Lnet/minecraft/entity/Entity;)V"
            )
            callEvent()
        })
    }
}
