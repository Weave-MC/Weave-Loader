package net.weavemc.api.event

import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.render.command.OrderedRenderCommandQueue
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.client.render.state.CameraRenderState
import net.minecraft.client.util.math.MatrixStack

/**
 * Fired when a living entity model is being rendered.
 *
 * @property renderer The entity renderer handling this living entity.
 * @property state The render state containing visual properties snapshot for this frame.
 * @property matrices The active 3D transformation matrix stack.
 * @property queue The command queue responsible for ordered draw passes.
 * @property cameraState The current frame camera transformation state.
 * @property tickCounter Frame timing provider retrieved from the active client instance.
 */
public sealed class RenderLivingEvent(
    public val renderer: LivingEntityRenderer<*, *, *>,
    public val state: LivingEntityRenderState,
    public val matrices: MatrixStack,
    public val queue: OrderedRenderCommandQueue,
    public val cameraState: CameraRenderState,
    public val tickCounter: RenderTickCounter
) : CancellableEvent() {
    /**
     * This is called before an entity is rendered.
     *
     * If cancelled, the entity is not rendered.
     */
    public class Pre(
        renderer: LivingEntityRenderer<*, *, *>,
        state: LivingEntityRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        cameraState: CameraRenderState,
        tickCounter: RenderTickCounter
    ) : RenderLivingEvent(renderer, state, matrices, queue, cameraState, tickCounter)

    /**
     * This is called after an entity is rendered.
     */
    public class Post(
        renderer: LivingEntityRenderer<*, *, *>,
        state: LivingEntityRenderState,
        matrices: MatrixStack,
        queue: OrderedRenderCommandQueue,
        cameraState: CameraRenderState,
        tickCounter: RenderTickCounter
    ) : RenderLivingEvent(renderer, state, matrices, queue, cameraState, tickCounter)
}