package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.ChatEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import net.weavemc.internals.prev
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode

internal class ChatEventSentHook : Hook("net/minecraft/client/network/ClientPlayNetworkHandler") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        transformSendChatMessage(node)
        transformSendChatCommandUnsigned(node)
        transformSendChatCommandSigned(node)

        cfg.computeFrames()
    }

    /**
     * @see net.minecraft.client.network.ClientPlayNetworkHandler.sendChatMessage
     */
    private fun transformSendChatMessage(node: ClassNode) {
        val instructions = node.methods.named("sendChatMessage").instructions

        val target = instructions.findTarget("net/minecraft/network/packet/c2s/play/ChatMessageC2SPacket")

        instructions.insertBefore(target, asm {
            new(internalNameOf<ChatEvent.Sent.Chat>())
            dup
            aload(1)
            aload(6)
            aload(2)
            invokespecial(
                internalNameOf<ChatEvent.Sent.Chat>(),
                "<init>",
                "(Ljava/lang/String;Lnet/minecraft/network/message/MessageSignatureData;Ljava/time/Instant;)V"
            )
            dup
            postEvent()
            dup

            returnIfEventCancelled(asm {
                pop
                _return
            })

            dup
            invokevirtual(
                internalNameOf<ChatEvent.Sent.Chat>(),
                "getMessage",
                "()Ljava/lang/String;"
            )
            astore(1)
            invokevirtual(
                internalNameOf<ChatEvent.Sent.Chat>(),
                "getSignature",
                "()Lnet/minecraft/network/message/MessageSignatureData;"
            )
            astore(6)
        })
    }

    /**
     * @see net.minecraft.client.network.ClientPlayNetworkHandler.sendChatCommand
     */
    private fun transformSendChatCommandUnsigned(node: ClassNode) {
        val instructions = node.methods.named("sendChatCommand").instructions

        val target = instructions.findTarget("net/minecraft/network/packet/c2s/play/CommandExecutionC2SPacket")

        instructions.insertBefore(target, asm {
            new(internalNameOf<ChatEvent.Sent.Command>())
            dup
            aload(1)
            iconst_0
            invokespecial(
                internalNameOf<ChatEvent.Sent.Command>(),
                "<init>",
                "(Ljava/lang/String;Z)V"
            )
            dup
            postEvent()
            dup

            returnIfEventCancelled(asm {
                pop
                _return
            })

            invokevirtual(
                internalNameOf<ChatEvent.Sent.Command>(),
                "getMessage",
                "()Ljava/lang/String;"
            )
            astore(1)
        })
    }

    /**
     * @see net.minecraft.client.network.ClientPlayNetworkHandler.sendChatCommand
     */
    private fun transformSendChatCommandSigned(node: ClassNode) {
        val instructions = node.methods.named("sendChatCommand").instructions

        val target = instructions.findTarget("net/minecraft/network/packet/c2s/play/ChatCommandSignedC2SPacket")

        instructions.insertBefore(target, asm {
            new(internalNameOf<ChatEvent.Sent.Command>())
            dup
            aload(1)
            iconst_1
            aload(7)
            aload(3)
            invokespecial(
                internalNameOf<ChatEvent.Sent.Command>(),
                "<init>",
                "(Ljava/lang/String;ZLnet/minecraft/network/message/ArgumentSignatureDataMap;Ljava/time/Instant;)V"
            )
            dup
            postEvent()
            dup

            returnIfEventCancelled(asm {
                pop
                _return
            })

            dup
            invokevirtual(
                internalNameOf<ChatEvent.Sent.Command>(),
                "getMessage",
                "()Ljava/lang/String;"
            )
            astore(1)
            invokevirtual(
                internalNameOf<ChatEvent.Sent.Command>(),
                "getArgumentSignatures",
                "()Lnet/minecraft/network/message/ArgumentSignatureDataMap;"
            )
            astore(7)
        })
    }

    private fun InsnList.findTarget(packetDesc: String): VarInsnNode? =
        first { it is TypeInsnNode && it.opcode == Opcodes.NEW && it.desc == packetDesc }
            .prev<VarInsnNode> { it.opcode == Opcodes.ALOAD && it.`var` == 0 }
}
