package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.LabelNode

internal fun HookManager.registerDefaultHooks() {

    register("net/minecraft/client/Minecraft") {
        val handlerMethod = node.generateMethod(desc = "()V") {
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
            f_same1(INTEGER)

            new("club/maxstats/weave/loader/api/event/InputEvent")
            dup_x1
            swap
            invokespecial("club/maxstats/weave/loader/api/event/InputEvent", "<init>", "(I)V")

            getObject<EventBus>()
            swap
            invokevirtual(
                internalNameOf<EventBus>(),
                "callEvent",
                "(L${internalNameOf<Event>()};)V"
            )

            +endOfIf
            f_same()
            _return
        }

        node.methods.add(handlerMethod)
        methodTransform(node.methods.named("runTick")) {
            callAdvice(
                matcher = { _, name, _ -> name == "dispatchKeypresses" },
                afterCall = {
                    visitASM {
                        aload(0)
                        invokevirtual(node.name, handlerMethod.name, "()V")
                    }
                }
            )
        }
    }

}