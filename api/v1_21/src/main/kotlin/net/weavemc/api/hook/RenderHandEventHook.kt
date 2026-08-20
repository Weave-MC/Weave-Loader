package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.RenderHandEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.search
import org.objectweb.asm.tree.ClassNode

internal class RenderHandEventHook : Hook("net/minecraft/client/render/item/HeldItemRenderer") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        /**
         * @see net.minecraft.client.render.item.HeldItemRenderer.renderItem
         */
        node.methods.search(
            "renderItem",
            "(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"
        ).instructions.insert(asm {
            new(internalNameOf<RenderHandEvent>())
            dup
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
                internalNameOf<RenderHandEvent>(),
                "<init>",
                "(Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
            dup
            postEvent()

            returnIfEventCancelled()
        })
    }
}