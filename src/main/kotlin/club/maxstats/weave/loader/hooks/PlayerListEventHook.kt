package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.api.event.PlayerListEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.MethodInsnNode

fun HookManager.registerPlayerListEventHook() = register("net/minecraft/client/network/NetHandlerPlayClient") {
    val addInsn = asm {
        new(internalNameOf<PlayerListEvent.Add>())
        dup
        aload(1)
        invokespecial(
            internalNameOf<PlayerListEvent.Add>(),
            "<init>",
            "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem\$AddPlayerData;)V"
        )
        getSingleton<EventBus>()
        swap

        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    }

    val removeInsn = asm {
        new(internalNameOf<PlayerListEvent.Remove>())
        dup
        aload(1)
        invokespecial(
            internalNameOf<PlayerListEvent.Remove>(),
            "<init>",
            "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem\$AddPlayerData;)V"
        )
        getSingleton<EventBus>()
        swap

        invokevirtual(
            internalNameOf<EventBus>(),
            "callEvent",
            "(L${internalNameOf<Event>()};)V"
        )
    }

    val mn = node.methods.named("handlePlayerListItem")
    mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "put" }, addInsn)
    mn.instructions.insertBefore(mn.instructions.find { it is MethodInsnNode && it.name == "remove" }, removeInsn)
}