package net.weavemc.api.event

/**
 * Cancellable event, called in [net.minecraft.client.renderer.EntityRenderer.renderWorld] in the event
 * that your hand is rendered (crazy right).
 */
class RenderHandEvent(val partialTicks: Float) : CancellableEvent()