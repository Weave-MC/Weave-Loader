package net.weavemc.api.event

import com.mojang.blaze3d.buffers.GpuBufferSlice
import net.minecraft.client.render.Camera
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.util.ObjectAllocator
import org.joml.Matrix4f
import org.joml.Vector4f

/**
 * World rendering lifecycle events fired surrounding [net.minecraft.client.render.WorldRenderer.render].
 *
 * @property allocator Memory allocator instance for render buffers.
 * @property tickCounter Timing provider containing frame progress metrics.
 * @property renderBlockOutline Whether block selection outline rendering is enabled.
 * @property camera The active view camera transformation state.
 * @property positionMatrix World position transformation matrix.
 * @property projectionMatrix Active camera projection matrix.
 * @property fogBuffer Active GPU buffer slice for fog parameters.
 * @property fogColour Active frame fog colour vector.
 */
public sealed class RenderWorldEvent(
    public val allocator: ObjectAllocator,
    public val tickCounter: RenderTickCounter,
    public val renderBlockOutline: Boolean,
    public val camera: Camera,
    public val positionMatrix: Matrix4f,
    public val projectionMatrix: Matrix4f,
    public val fogBuffer: GpuBufferSlice,
    public val fogColour: Vector4f
) : CancellableEvent() {
    /**
     * Fired before world rendering begins, before camera frustum setup and geometry passes execute.
     */
    public class Pre(
        allocator: ObjectAllocator,
        tickCounter: RenderTickCounter,
        renderBlockOutline: Boolean,
        camera: Camera,
        positionMatrix: Matrix4f,
        projectionMatrix: Matrix4f,
        fogBuffer: GpuBufferSlice,
        fogColour: Vector4f
    ) : RenderWorldEvent(allocator, tickCounter, renderBlockOutline, camera, positionMatrix, projectionMatrix, fogBuffer, fogColour)

    /**
     * Fired after all world rendering passes, post-processing, and frame clean-up are complete.
     */
    public class Post(
        allocator: ObjectAllocator,
        tickCounter: RenderTickCounter,
        renderBlockOutline: Boolean,
        camera: Camera,
        positionMatrix: Matrix4f,
        projectionMatrix: Matrix4f,
        fogBuffer: GpuBufferSlice,
        fogColour: Vector4f
    ) : RenderWorldEvent(allocator, tickCounter, renderBlockOutline, camera, positionMatrix, projectionMatrix, fogBuffer, fogColour)
}