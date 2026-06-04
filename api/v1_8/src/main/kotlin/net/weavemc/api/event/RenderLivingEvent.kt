package net.weavemc.api.event

import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase

/**
 * This event is called when an entity is rendered.
 *
 * It is split into [RenderLivingEvent.Pre] and [RenderLivingEvent.Post]. The [RenderLivingEvent.Pre] version of this event is cancellable.
 *
 * @property entity The entity being rendered.
 * @property x The `x` coordinate where the entity is being rendered this frame.
 * @property y The `y` coordinate where the entity is being rendered this frame.
 * @property z The `z` coordinate where the entity is being rendered this frame.
 */
public sealed class RenderLivingEvent(
    public val renderer: RendererLivingEntity<EntityLivingBase>,
    public val entity: EntityLivingBase,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val partialTicks: Float
) : CancellableEvent() {
    /**
     * This is called before an entity is rendered.
     *
     * If cancelled, the entity is not rendered.
     */
    public class Pre(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

    /**
     * This is called after an entity is rendered.
     */
    public class Post(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)
}