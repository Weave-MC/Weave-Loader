@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.getSingleton
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.StartGameEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [StartGameEvent.Pre] and [StartGameEvent.Post].
 */
class StartGameEventHook : Hook(getMappedClass("net/minecraft/client/Minecraft")) {

    /**
     * Inserts a call in [net.minecraft.client.Minecraft.startGame] to [StartGameEvent.Pre] and later [StartGameEvent.Post].
     *
     * @see net.minecraft.client.Minecraft.startGame
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "startGame",
            "()V"
        ) ?: error("Failed to find mapping for Minecraft#startGame")

        val startGame = node.methods.search(mappedMethod.name, mappedMethod.descriptor)

        startGame.instructions.insert(asm {
            getSingleton<StartGameEvent.Pre>()
            callEvent()
        })

        startGame.instructions.insertBefore(startGame.instructions.findLast { it.opcode == RETURN }, asm {
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
