@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.getSingleton
import net.weavemc.weave.api.bytecode.named
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
