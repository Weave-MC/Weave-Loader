package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.CancellableEvent
import club.maxstats.weave.loader.api.event.ChatReceivedEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.LabelNode

fun HookManager.registerChatReceivedHook() = register("net/minecraft/client/gui/GuiNewChat") {
    node.methods.named("printChatMessageWithOptionalDeletion").instructions.insert(asm {
        new(internalNameOf<ChatReceivedEvent>())
        dup
        dup
        aload(1)
        invokespecial(
            internalNameOf<ChatReceivedEvent>(),
            "<init>",
            "(Lnet/minecraft/util/IChatComponent;)V"
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
