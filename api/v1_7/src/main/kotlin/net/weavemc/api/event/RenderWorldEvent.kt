package net.weavemc.api.event

/**
 * Non-cancellable, called in [net.minecraft.client.renderer.EntityRenderer.renderWorld] after the world is rendered.
 * It is also called few lines before [RenderHandEvent].
 */
class RenderWorldEvent(val partialTicks: Float) : Event()
