package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.RenderLivingEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

internal class RenderLivingEventHook : Hook("net/minecraft/client/render/entity/LivingEntityRenderer") {
    /**
     * @see net.minecraft.client.render.entity.LivingEntityRenderer.render
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val instructions = node.methods.named("render").instructions

        instructions.insert(generateInstructions(internalNameOf<RenderLivingEvent.Pre>()))
        instructions.insertBefore(instructions.last { it.opcode == Opcodes.RETURN }, generateInstructions(internalNameOf<RenderLivingEvent.Post>()))
    }

    private fun generateInstructions(eventName: String) = asm {
        new(eventName)
        dup
        aload(0) // LivingEntityRenderer
        aload(1) // LivingEntityRenderState
        aload(2) // MatrixStack
        aload(3) // OrderedRenderCommandQueue
        aload(4) // CameraRenderState
        invokestatic(
            "net/minecraft/client/MinecraftClient",
            "getInstance",
            "()Lnet/minecraft/client/MinecraftClient;"
        )
        invokevirtual(
            "net/minecraft/client/MinecraftClient",
            "getRenderTickCounter",
            "()Lnet/minecraft/client/render/RenderTickCounter;"
        )
        invokespecial(
            eventName,
            "<init>",
            "(Lnet/minecraft/client/render/entity/LivingEntityRenderer;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;Lnet/minecraft/client/render/RenderTickCounter;)V"
        )
        dup
        postEvent()

        returnIfEventCancelled()
    }
}