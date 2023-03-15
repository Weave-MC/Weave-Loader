package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.EntityListEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.Opcodes

fun HookManager.registerEntityListAddHook() = register("net/minecraft/world/World") {
    node.methods.named("spawnEntityInWorld").instructions.insert(asm {
        new(internalNameOf<EntityListEvent.Add>())
        dup
        aload(1)
        invokespecial(
            internalNameOf<EntityListEvent.Add>(),
            "<init>",
            "(Lnet/minecraft/entity/Entity;)V"
        )
        callEvent()
    })
}

fun HookManager.registerEntityListRemoveHook() = register("net/minecraft/client/multiplayer/WorldClient") {
    val mn = node.methods.named("removeEntityFromWorld")
    mn.instructions.insert(mn.instructions.find { it.opcode == Opcodes.IFNULL }, asm {
        new(internalNameOf<EntityListEvent.Remove>())
        dup
        aload(2)
        invokespecial(
            internalNameOf<EntityListEvent.Remove>(),
            "<init>",
            "(Lnet/minecraft/entity/Entity;)V"
        )
        callEvent()
    })
}
