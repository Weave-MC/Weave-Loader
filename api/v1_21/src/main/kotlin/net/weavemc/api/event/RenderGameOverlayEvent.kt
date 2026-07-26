package net.weavemc.api.event

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter

/**
 * Base event fired when the in-game HUD (game overlay) is being rendered.
 *
 * @property drawContext The active 2D rendering context providing canvas draw routines.
 * @property tickCounter Timing provider containing frame progress and delta metrics.
 */
public sealed class RenderGameOverlayEvent(
    public val drawContext: DrawContext,
    public val tickCounter: RenderTickCounter
) : Event() {
    /**
     * Fired **before** the vanilla in-game HUD elements (hotbar, health, crosshair, etc.) are rendered.
     *
     * Use this event to render custom elements underneath vanilla UI or to prepare custom batch state.
     */
    public class Pre(
        drawContext: DrawContext,
        tickCounter: RenderTickCounter
    ) : RenderGameOverlayEvent(drawContext, tickCounter)

    /**
     * Fired **after** all vanilla in-game HUD elements have finished rendering.
     *
     * Use this event to render custom HUD components, module overlays, or debug statistics over the game interface.
     */
    public class Post(
        drawContext: DrawContext,
        tickCounter: RenderTickCounter
    ) : RenderGameOverlayEvent(drawContext, tickCounter)
}