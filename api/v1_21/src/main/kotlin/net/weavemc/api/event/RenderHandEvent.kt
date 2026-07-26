package net.weavemc.api.event

import net.minecraft.client.render.RenderTickCounter

/**
 * Cancellable event called in [net.minecraft.client.render.item.HeldItemRenderer] when held items are rendered.
 *
 * @property tickCounter Frame timing provider.
 */
public class RenderHandEvent(public val tickCounter: RenderTickCounter) : CancellableEvent()