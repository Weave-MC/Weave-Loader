package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.RenderWorldEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

internal class RenderWorldEventHook : Hook("net/minecraft/client/render/WorldRenderer") {
    /**
     * @see net.minecraft.client.render.WorldRenderer.render
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val instructions = node.methods.named("render").instructions

        instructions.insert(generateInstructions(internalNameOf<RenderWorldEvent.Pre>()))
        instructions.insertBefore(instructions.last { it.opcode == Opcodes.RETURN }, generateInstructions(internalNameOf<RenderWorldEvent.Post>()))

        cfg.computeFrames()
    }

    private fun generateInstructions(eventName: String) = asm {
        new(eventName)
        dup
        aload(1)  // ObjectAllocator
        aload(2)  // RenderTickCounter
        iload(3)  // renderBlockOutline (boolean)
        aload(4)  // Camera
        aload(5)  // positionMatrix (Matrix4f)
        aload(6)  // projectionMatrix (Matrix4f)
        aload(8)  // fogBuffer (GpuBufferSlice)
        aload(9)  // fogColor (Vector4f)
        invokespecial(
            eventName,
            "<init>",
            "(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;)V"
        )
        dup
        postEvent()

        returnIfEventCancelled()
    }
}