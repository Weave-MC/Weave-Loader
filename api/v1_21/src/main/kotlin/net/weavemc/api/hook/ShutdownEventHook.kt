package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.ShutdownEvent
import net.weavemc.internals.asm
import net.weavemc.internals.getSingleton
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

internal class ShutdownEventHook : Hook("net/minecraft/client/MinecraftClient") {
    /**
     * Inserts a call to post [ShutdownEvent] at the head of [net.minecraft.client.MinecraftClient.stop].
     *
     * @see net.minecraft.client.MinecraftClient.stop
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("stop").instructions.insert(asm {
            getSingleton<ShutdownEvent>()
            postEvent()
        })
    }
}
