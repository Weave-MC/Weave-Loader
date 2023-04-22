package net.weavemc.loader.api.event.client

import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase
import net.weavemc.loader.api.event.CancellableEvent
import net.weavemc.loader.api.event.Event

public sealed class RenderGameOverlayEvent(public val partialTicks: Float) : Event() {
    public class Pre(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)

    public class Post(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)
}

public class RenderHandEvent(public val partialTicks: Float) : CancellableEvent()

public sealed class RenderLivingEvent(
    public val renderer: RendererLivingEntity<EntityLivingBase>,
    public val entity: EntityLivingBase,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val partialTicks: Float
) : CancellableEvent() {
    public class Pre(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

    public class Post(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)
}

public class RenderWorldEvent(public val partialTicks: Float) : Event()
