package net.weavemc.loader.hooks

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.client.EntityJoinWorldEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

internal class EntityJoinWorldEventHook : Hook("net/minecraft/world/World") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val methodNode = node.methods.named("spawnEntityInWorld")

        val instruction = methodNode.instructions.find { insnNode ->
            insnNode is VarInsnNode && insnNode.opcode == Opcodes.ALOAD && insnNode.`var` == 0
                && insnNode.next.let { nextInsnNode ->
                nextInsnNode != null && nextInsnNode is MethodInsnNode && nextInsnNode.opcode == Opcodes.INVOKEVIRTUAL && nextInsnNode.owner == "net/minecraft/world/World" && nextInsnNode.name == "updateAllPlayersSleepingFlag"
                    && nextInsnNode.next.let { nextNextInsnNode ->
                    nextNextInsnNode != null && nextNextInsnNode is LabelNode
                }
            }
        }

        if (instruction != null) {
            methodNode.instructions.insert(instruction.next.next, asm {
                new(internalNameOf<EntityJoinWorldEvent>())
                dup
                dup
                aload(1)
                aload(0)
                invokespecial(
                    internalNameOf<EntityJoinWorldEvent>(),
                    "<init>",
                    "(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/World;)V"
                )
                callEvent()

                val end = LabelNode()

                invokevirtual(internalNameOf<EntityJoinWorldEvent>(), "isCancelled", "()Z")
                ifeq(end)

                iload(4)
                ifne(end)

                iconst_0
                ireturn

                +end
            })

            cfg.computeFrames()
        }
    }
}
