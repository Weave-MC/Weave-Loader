package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.CommandEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

internal class CommandEventRegisterHook : Hook("net/minecraft/client/network/ClientPlayNetworkHandler") {
    /**
     * Inserts a call to post [CommandEvent.Register] inside [net.minecraft.client.network.ClientPlayNetworkHandler.onCommandTree].
     *
     * @see net.minecraft.client.network.ClientPlayNetworkHandler.onCommandTree
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val instructions = node.methods.named("onCommandTree").instructions

        instructions.insertBefore(instructions.last { it.opcode == Opcodes.RETURN }, asm {
            new(internalNameOf<CommandEvent.Register>())
            dup
            aload(0)
            getfield(
                "net/minecraft/client/network/ClientPlayNetworkHandler",
                "commandDispatcher",
                "Lcom/mojang/brigadier/CommandDispatcher;"
            )
            aload(0)
            invokevirtual(
                "net/minecraft/client/network/ClientPlayNetworkHandler",
                "getRegistryManager",
                "()Lnet/minecraft/registry/DynamicRegistryManager\$Immutable;"
            )
            invokespecial(
                internalNameOf<CommandEvent.Register>(),
                "<init>",
                "(Lcom/mojang/brigadier/CommandDispatcher;Lnet/minecraft/command/CommandRegistryAccess;)V"
            )
            postEvent()
        })
    }
}