package net.weavemc.api.event

import net.minecraft.client.Minecraft

/**
 * Non-cancellable event, split into [StartGameEvent.Pre] and [StartGameEvent.Post].
 */
sealed class StartGameEvent : Event() {
    /**
     * Called in correspondence with [Minecraft.startGame] at the head of the method.
     * Therefore, [StartGameEvent.Pre] is called early in the game startup process.
     */
    object Pre : StartGameEvent()

    /**
     * Called in correspondence with [Minecraft.startGame] at the tail of the method.
     * Therefore, [StartGameEvent.Post] is called late in the game startup process.
     */
    object Post : StartGameEvent()
}