package club.maxstats.weave.loader.hooks.impl

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.generateMethodName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class InputEventHook : Hook("net/minecraft/client/Minecraft") {
    override fun transform(cn: ClassNode, callback: Callback) {
        //added a new method to make doing frames easier
        val mn = MethodNode(
            Opcodes.ACC_PUBLIC,
            cn.generateMethodName(),
            "()V",
            null,
            null
        )
        mn.instructions = asm {
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

            new("club/maxstats/weave/loader/api/event/impl/InputEvent")
            dup_x1
            swap
            invokespecial("club/maxstats/weave/loader/api/event/impl/InputEvent", "<init>", "(I)V")

            getstatic(
                "club/maxstats/weave/loader/api/event/EventBus",
                "INSTANCE",
                "Lclub/maxstats/weave/loader/api/event/EventBus;"
            )
            swap
            invokevirtual(
                "club/maxstats/weave/loader/api/event/EventBus",
                "callEvent",
                "(Lclub/maxstats/weave/loader/api/event/Event;)V"
            )

            +endOfIf
            f_same()
            _return
        }
        cn.methods.add(mn)

        val runTick = cn.methods.find { it.name == "runTick" }!!
        runTick.instructions.insert(
            runTick.instructions.find { it is MethodInsnNode && it.name == "dispatchKeypresses" },
            asm {
                aload(0)
                invokevirtual(cn.name, mn.name, "()V")
            }
        )
    }
}