@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.getSingleton
import net.weavemc.api.bytecode.named
import net.weavemc.api.event.StartGameEvent
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [StartGameEvent.Pre] and [StartGameEvent.Post].
 */
internal class StartGameEventHook : Hook("net/minecraft/client/Minecraft") {

    /**
     * Inserts a call in [net.minecraft.client.Minecraft.startGame] to [StartGameEvent.Pre] and later [StartGameEvent.Post].
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
