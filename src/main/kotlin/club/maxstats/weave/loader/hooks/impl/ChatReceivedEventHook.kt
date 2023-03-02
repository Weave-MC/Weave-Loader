package club.maxstats.weave.loader.hooks.impl

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.tree.ClassNode

class ChatReceivedEventHook : Hook("net/minecraft/client/gui/GuiNewChat") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("printChatMessageWithOptionalDeletion").instructions.insert(asm {
            new("club/maxstats/weave/loader/api/event/ChatReceivedEvent")
            dup
            aload(1)
            invokespecial("club/maxstats/weave/loader/api/event/ChatReceivedEvent", "<init>", "(Lnet/minecraft/util/IChatComponent;)V")
            getSingleton<EventBus>()
            swap
            invokevirtual(
                internalNameOf<EventBus>(),
                "callEvent",
                "(L${internalNameOf<Event>()};)V"
            )
        })
    }
}