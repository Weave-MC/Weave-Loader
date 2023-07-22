@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.getSingleton
import net.weavemc.weave.api.bytecode.named
import net.weavemc.weave.api.event.ShutdownEvent
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ShutdownEvent].
 */
internal class ShutdownEventHook : Hook("net/minecraft/client/Minecraft") {

    /**
     * Inserts a singleton shutdown call at the head of
     * [net.minecraft.client.Minecraft.shutdownMinecraftApplet].
     *
     * @see net.minecraft.client.Minecraft.shutdownMinecraftApplet
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("shutdownMinecraftApplet").instructions.insert(asm {
            getSingleton<ShutdownEvent>()
            callEvent()
        })
    }
}
