@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.*
import net.weavemc.weave.api.event.EntityListEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

internal class EntityListEventAddHook : Hook("net/minecraft/world/World") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/world/World",
            "spawnEntityInWorld",
            "(Lnet/minecraft/entity/Entity;)Z"
        )

        node.methods.search(mappedMethod.runtimeName, mappedMethod.desc).instructions.insert(asm {
            new(internalNameOf<EntityListEvent.Add>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<EntityListEvent.Add>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/entity/Entity")};)V"
            )
            callEvent()
        })
    }
}

internal class EntityListEventRemoveHook : Hook("net/minecraft/client/multiplayer/WorldClient") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/multiplayer/WorldClient",
            "removeEntityFromWorld",
            "(I)Lnet/minecraft/entity/Entity;"
        )

        val mn = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)
        mn.instructions.insert(mn.instructions.find { it.opcode == Opcodes.IFNULL }, asm {
            new(internalNameOf<EntityListEvent.Remove>())
            dup
            aload(2)
            invokespecial(
                internalNameOf<EntityListEvent.Remove>(),
                "<init>",
                "(L${getMappedClass("net/minecraft/entity/Entity")};)V"
            )
            callEvent()
        })
    }
}
