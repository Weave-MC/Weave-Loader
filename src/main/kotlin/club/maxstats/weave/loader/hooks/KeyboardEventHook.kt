package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.KeyboardEvent
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode

fun HookManager.registerKeyboardHook() = register("net/minecraft/client/Minecraft") {
    val handler = node.generateMethod(desc = "()V") {
        val end = LabelNode()

        invokestatic("org/lwjgl/input/Keyboard", "getEventKeyState", "()Z")
        ifeq(end)

        aload(0)
        getfield("net/minecraft/client/Minecraft", "currentScreen", "Lnet/minecraft/client/gui/GuiScreen;")
        ifnonnull(end)

        new(internalNameOf<KeyboardEvent>())
        dup
        invokespecial(
            internalNameOf<KeyboardEvent>(),
            "<init>",
            "()V"
        )
        callEvent()

        +end
        f_same()
        _return
    }

    node.methods.named("runTick").let { mn ->
        mn.instructions.insert(
            mn.instructions.find { it is MethodInsnNode && it.name == "dispatchKeypresses" },
            asm {
                aload(0)
                invokevirtual(node.name, handler.name, "()V")
            }
        )
    }
}
