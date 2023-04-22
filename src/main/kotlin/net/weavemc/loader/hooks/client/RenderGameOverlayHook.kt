package net.weavemc.loader.hooks.client

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.client.RenderGameOverlayEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

internal class RenderGameOverlayHook : Hook("net/minecraft/client/renderer/EntityRenderer") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("updateCameraAndRender")

        val renderGameOverlayCall = mn.instructions.find {
            it is MethodInsnNode && it.name == "renderGameOverlay"
        }

        mn.instructions.insertBefore(renderGameOverlayCall, asm {
            new(internalNameOf<RenderGameOverlayEvent.Pre>())
            dup
            fload(1)
            invokespecial(
                internalNameOf<RenderGameOverlayEvent.Pre>(),
                "<init>",
                "(F)V"
            )
            callEvent()
        })

        mn.instructions.insert(renderGameOverlayCall, asm {
            new(internalNameOf<RenderGameOverlayEvent.Post>())
            dup
            fload(1)
            invokespecial(
                internalNameOf<RenderGameOverlayEvent.Post>(),
                "<init>",
                "(F)V"
            )
            callEvent()
        })
    }
}
