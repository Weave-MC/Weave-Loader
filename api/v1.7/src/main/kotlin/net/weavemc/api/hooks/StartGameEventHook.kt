@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.getSingleton
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.StartGameEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
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
        )

        val startGame = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)

        startGame.instructions.insert(asm {
            getSingleton<net.weavemc.api.event.StartGameEvent.Pre>()
            callEvent()
        })

        startGame.instructions.insertBefore(startGame.instructions.findLast { it.opcode == RETURN }, asm {
            getSingleton<net.weavemc.api.event.StartGameEvent.Post>()
            callEvent()
            invokestatic(
                "net/weavemc/loader/analytics/AnalyticsKt",
                "updateLaunchTimes",
                "()V"
            )
        })
    }
}
