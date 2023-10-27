@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.ServerConnectEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ServerConnectEvent].
 */
class ServerConnectEventHook : Hook(getMappedClass("net/minecraft/client/multiplayer/GuiConnecting")) {

    /**
     * Inserts a call to [ServerConnectEvent]'s constructor at the head of
     * [net.minecraft.client.multiplayer.GuiConnecting.connect]. Triggered in the
     * event which [net.minecraft.client.multiplayer.GuiConnecting.connect] is called,
     * which is called when the player clicks the 'connect' button in the server list.
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/multiplayer/GuiConnecting",
            "connect",
            "(Ljava/lang/String;I)V"
        )

        node.methods.search(mappedMethod.runtimeName, mappedMethod.desc).instructions.insert(asm {
            new(internalNameOf<net.weavemc.api.event.ServerConnectEvent>())
            dup
            aload(1)
            iload(2)
            invokespecial(
                internalNameOf<net.weavemc.api.event.ServerConnectEvent>(),
                "<init>",
                "(Ljava/lang/String;I)V"
            )

            callEvent()
        })
    }
}
