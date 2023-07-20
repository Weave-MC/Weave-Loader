@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ServerConnectEvent].
 */
internal class ServerConnectEventHook : Hook("net/minecraft/client/multiplayer/GuiConnecting") {

    /**
     * Inserts a call to [ServerConnectEvent]'s constructor at the head of
     * [net.minecraft.client.multiplayer.GuiConnecting.connect]. Triggered in the
     * event which [net.minecraft.client.multiplayer.GuiConnecting.connect] is called,
     * which is called when the player clicks the 'connect' button in the server list.
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
