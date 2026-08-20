package net.weavemc.api.event

/**
 * Non-cancellable event, split into [StartGameEvent.Pre] and [StartGameEvent.Post].
 */
public sealed class StartGameEvent : Event() {
    /**
     * Called at the head of [net.minecraft.client.MinecraftClient.run], early in the game startup process.
     */
    public object Pre : StartGameEvent()

    /**
     * Called at the tail of [net.minecraft.client.MinecraftClient.run], late in the game startup process.
     */
    public object Post : StartGameEvent()
}