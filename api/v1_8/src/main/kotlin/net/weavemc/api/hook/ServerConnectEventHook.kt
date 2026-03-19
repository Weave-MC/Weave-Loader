package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.ServerConnectEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ServerConnectEvent].
 */
internal class ServerConnectEventHook : Hook("net/minecraft/client/multiplayer/GuiConnecting") {
    /**
     * Inserts a call to [ServerConnectEvent] at the head of [net.minecraft.client.multiplayer.GuiConnecting.connect].
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("connect").instructions.insert(asm {
            new(internalNameOf<ServerConnectEvent>())
            dup
            aload(1)
            iload(2)
            invokespecial(
                internalNameOf<ServerConnectEvent>(),
                "<init>",
                "(Ljava/lang/String;I)V"
            )

            postEvent()
        })
    }
}
