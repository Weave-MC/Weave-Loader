package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.ShutdownEvent
import club.maxstats.weave.loader.api.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.ClassNode

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
