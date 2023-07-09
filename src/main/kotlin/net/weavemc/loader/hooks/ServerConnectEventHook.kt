package net.weavemc.loader.hooks

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.ServerConnectEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
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

            callEvent()
        })
    }
}
