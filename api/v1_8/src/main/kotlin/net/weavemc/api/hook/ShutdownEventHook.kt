package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.ShutdownEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.internals.asm
import net.weavemc.internals.getSingleton
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ShutdownEvent].
 */
internal object ShutdownEventHook : Hook("net/minecraft/client/Minecraft") {
    /**
     * Inserts a call to
     * [net.minecraft.client.Minecraft.shutdownMinecraftApplet].
     * at the head of [net.minecraft.client.Minecraft.shutdownMinecraftApplet].
     *
     * @see net.minecraft.client.Minecraft.shutdownMinecraftApplet
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("shutdownMinecraftApplet").instructions.insert(asm {
            getSingleton<ShutdownEvent>()
            postEvent()
        })
    }
}
