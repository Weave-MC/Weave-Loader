@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.ServerConnectEvent
import net.weavemc.weave.api.not
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ServerConnectEvent].
 */
class ServerConnectEventHook : Hook(!"net/minecraft/client/multiplayer/GuiConnecting") {

    /**
     * Inserts a call to [ServerConnectEvent]'s constructor at the head of
     * [net.minecraft.client.multiplayer.GuiConnecting.connect]. Triggered in the
     * event which [net.minecraft.client.multiplayer.GuiConnecting.connect] is called,
     * which is called when the player clicks the 'connect' button in the server list.
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.search(!"connect", "V", "Ljava/lang/String;", "I").instructions.insert(asm {
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
