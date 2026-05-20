package net.weavemc.api.event

/**
 * This event is called when the HUD (game overlay) is being rendered.
 *
 * It is split into [RenderGameOverlayEvent.Pre] and [RenderGameOverlayEvent.Post].
 */
sealed class RenderGameOverlayEvent(val partialTicks: Float) : Event() {
    /**
     * This is called **before** the game overlay renders, and should be used if you want to
     * draw to the screen without drawing over the HUD.
     */
    class Pre(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)

    /**
     * This is called **after** the game overlay renders, and should be used to draw whatever
     * HUD components your mod needs.
     */
    class Post(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)
}