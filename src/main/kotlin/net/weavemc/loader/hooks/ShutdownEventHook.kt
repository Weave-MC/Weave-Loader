package net.weavemc.loader.hooks

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.ShutdownEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.getSingleton
import net.weavemc.loader.util.named
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
