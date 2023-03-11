package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.*
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.Opcodes

fun HookManager.registerRenderGameOverlayHook() = register("net/minecraft/client/gui/GuiIngame") {
    val preInsn = asm {
        new(internalNameOf<RenderGameOverlayEvent.Pre>())
        dup
        fload(1)
        invokespecial(
            internalNameOf<RenderGameOverlayEvent.Pre>(),
            "<init>",
            "(F)V"
        )
        callEvent()
    }

    val postInsn = asm {
        new(internalNameOf<RenderGameOverlayEvent.Post>())
        dup
        fload(1)
        invokespecial(
            internalNameOf<RenderGameOverlayEvent.Post>(),
            "<init>",
            "(F)V"
        )
        callEvent()
    }

    val mn = node.methods.named("renderGameOverlay")
    mn.instructions.insert(preInsn)
    mn.instructions.insertBefore(mn.instructions.find { it.opcode == Opcodes.RETURN}, postInsn)
}
