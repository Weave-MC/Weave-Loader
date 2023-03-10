package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.CancellableEvent
import club.maxstats.weave.loader.api.event.MouseEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.lwjgl.input.Mouse
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode

fun HookManager.registerMouseHook() = register("net/minecraft/client/Minecraft") {
    val mn = node.methods.named("runTick")

    val mouseNext = mn.instructions.find {
        it is MethodInsnNode && it.owner == internalNameOf<Mouse>() && it.name == "next"
    }!!

    val top = LabelNode()
    mn.instructions.insertBefore(mouseNext, top)
    mn.instructions.insert(mouseNext.next, asm {
        new(internalNameOf<MouseEvent>())
        dup; dup
        invokespecial(internalNameOf<MouseEvent>(), "<init>", "()V")
        callEvent()

        invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
        ifne(top)
    })
}
