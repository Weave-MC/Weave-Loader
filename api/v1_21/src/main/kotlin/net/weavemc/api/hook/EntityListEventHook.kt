package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.EntityListEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.VarInsnNode

internal class EntityListEventHook : Hook("net/minecraft/client/world/ClientWorld") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        transformAddEntity(node)
        transformRemoveEntity(node)
    }

    /**
     * @see net.minecraft.client.world.ClientWorld.addEntity
     */
    fun transformAddEntity(node: ClassNode) {
        node.methods.named("addEntity").instructions.insert(asm {
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

    /**
     * @see net.minecraft.client.world.ClientWorld.removeEntity
     */
    fun transformRemoveEntity(node: ClassNode) {
        val instructions = node.methods.named("removeEntity").instructions

        val entityVarIndex = 3
        val targetJump = instructions.first { it.opcode == Opcodes.IFNULL && it.previous.let { prev ->
            prev is VarInsnNode && prev.opcode == Opcodes.ALOAD && prev.`var` == entityVarIndex
        }}

        instructions.insert(targetJump, asm {
            new(internalNameOf<EntityListEvent.Remove>())
            dup
            aload(entityVarIndex)
            aload(2)
            invokespecial(
                internalNameOf<EntityListEvent.Remove>(),
                "<init>",
                "(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity\$RemovalReason;)V"
            )
            postEvent()
        })
    }
}