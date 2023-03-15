package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.KeyboardEvent
import club.maxstats.weave.loader.util.*
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode

fun HookManager.registerKeyboardHook() = register("net/minecraft/client/Minecraft") {
    node.methods.named("runTick").let { mn ->
        mn.instructions.insert(
            mn.instructions.find { it is MethodInsnNode && it.name == "dispatchKeypresses" },
            asm {
                new(internalNameOf<KeyboardEvent>())
                dup
                invokespecial(
                    internalNameOf<KeyboardEvent>(),
                    "<init>",
                    "()V"
                )
                callEvent()
            }
        )
    }
}
