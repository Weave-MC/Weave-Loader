package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.CancellableEvent
import club.maxstats.weave.loader.api.event.ChatSentEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.LabelNode

fun HookManager.registerChatSentHook() = register("net/minecraft/client/entity/EntityPlayerSP") {
    node.methods.named("sendChatMessage").instructions.insert(asm {
        new(internalNameOf<ChatSentEvent>())
        dup
        dup
        aload(1)
        invokespecial(
            internalNameOf<ChatSentEvent>(),
            "<init>",
            "(L${internalNameOf<String>()};)V"
        )
        callEvent()

        val end = LabelNode()

        invokevirtual(internalNameOf<CancellableEvent>(), "isCancelled", "()Z")
        ifeq(end)

        _return

        +end
        f_same()
    })
}
