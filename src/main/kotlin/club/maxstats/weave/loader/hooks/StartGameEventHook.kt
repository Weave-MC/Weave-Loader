package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.api.event.StartGameEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.Opcodes

fun HookManager.registerStartGameHook() = register("net/minecraft/client/Minecraft") {
    val preInsn = asm {
        getSingleton<EventBus>()
        getSingleton<StartGameEvent.Pre>()

        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    }

    val postInsn = asm {
        getSingleton<EventBus>()
        getSingleton<StartGameEvent.Post>()

        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    }

    val mn = node.methods.named("startGame")
    mn.instructions.insert(preInsn)
    mn.instructions.insertBefore(mn.instructions.find { it.opcode == Opcodes.RETURN }, postInsn)
}
