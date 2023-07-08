package net.weavemc.loader.hooks

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.StartGameEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.getSingleton
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [StartGameEvent.Pre] and [StartGameEvent.Post].
 */
internal class StartGameEventHook : Hook("net/minecraft/client/Minecraft") {

    /**
     * Inserts a call to [net.minecraft.client.Minecraft.startGame] using the Event Bus.
     *
     * [StartGameEvent.Pre] is called at the head of [net.minecraft.client.Minecraft.startGame]. Whereas
     * [StartGameEvent.Post] is called at the tail of [net.minecraft.client.Minecraft.startGame].
     *
     * @see net.minecraft.client.Minecraft.startGame
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("startGame")

        mn.instructions.insert(asm {
            getSingleton<StartGameEvent.Pre>()
            callEvent()
        })

        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == RETURN }, asm {
            getSingleton<StartGameEvent.Post>()
            callEvent()
            invokestatic(
                "net/weavemc/loader/analytics/AnalyticsKt",
                "updateLaunchTimes",
                "()V"
            )
        })
    }
}
