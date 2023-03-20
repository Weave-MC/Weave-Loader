package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.RenderGameOverlayEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

class RenderGameOverlayHook : Hook("net/minecraft/client/renderer/EntityRenderer") {
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
