@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.EntityListEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * @see net.minecraft.world.World.spawnEntityInWorld
 */
class EntityListEventAddHook : Hook(getMappedClass("net/minecraft/world/World")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/world/World",
            "spawnEntityInWorld",
            "(Lnet/minecraft/entity/Entity;)Z"
        )

        node.methods.search(mappedMethod.runtimeName, mappedMethod.desc).instructions.insert(asm {
            new(internalNameOf<net.weavemc.api.event.EntityListEvent.Add>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<net.weavemc.api.event.EntityListEvent.Add>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/entity/Entity")};)V"
            )
            callEvent()
        })
    }
}

/**
 * @see net.minecraft.client.multiplayer.WorldClient.removeEntityFromWorld
 */
class EntityListEventRemoveHook : Hook(getMappedClass("net/minecraft/client/multiplayer/WorldClient")) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/multiplayer/WorldClient",
            "removeEntityFromWorld",
            "(I)Lnet/minecraft/entity/Entity;"
        )

        val mn = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)
        mn.instructions.insert(mn.instructions.find { it.opcode == Opcodes.IFNULL }, asm {
            new(internalNameOf<net.weavemc.api.event.EntityListEvent.Remove>())
            dup
            aload(2)
            invokespecial(
                internalNameOf<net.weavemc.api.event.EntityListEvent.Remove>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/entity/Entity")};)V"
            )
            callEvent()
        })
    }
}
