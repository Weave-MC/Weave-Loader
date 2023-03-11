package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.InputEvent
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode

fun HookManager.registerInputHook() = register("net/minecraft/client/Minecraft") {
    val handler = node.generateMethod(desc = "()V") {
        val endOfIf = LabelNode()

        invokestatic("org/lwjgl/input/Keyboard", "getEventKeyState", "()Z")
        ifeq(endOfIf)

        aload(0)
        getfield("net/minecraft/client/Minecraft", "currentScreen", "Lnet/minecraft/client/gui/GuiScreen;")
        ifnonnull(endOfIf)

        val lambda = LabelNode()
        val end = LabelNode()

        invokestatic("org/lwjgl/input/Keyboard", "getEventKey", "()I")
        ifne(lambda)

        invokestatic("org/lwjgl/input/Keyboard", "getEventCharacter", "()C")
        sipush(256)
        iadd
        goto(end)

        +lambda
        f_same()

        invokestatic("org/lwjgl/input/Keyboard", "getEventKey", "()I")
        +end
        f_same1(Opcodes.INTEGER)

        new(internalNameOf<InputEvent>())
        dup_x1
        swap
        invokespecial(
            internalNameOf<InputEvent>(),
            "<init>",
            "(I)V"
        )

        callEvent()

        +endOfIf
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
