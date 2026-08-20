package net.weavemc.api.bytecode

import net.weavemc.api.event.CancellableEvent
import net.weavemc.api.event.Event
import net.weavemc.api.event.EventBus
import net.weavemc.internals.InsnBuilder
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode

/**
 * Pops an [net.weavemc.api.event.Event] off the stack and posts the [net.weavemc.api.event.Event] to the [EventBus]
 */
public fun InsnBuilder.postEvent() {
    invokestatic(
        internalNameOf<EventBus>(),
        "postEvent",
        "(L${internalNameOf<Event>()};)V"
    )
}

public fun InsnBuilder.isEventCancelled() {
    invokevirtual(
        internalNameOf<CancellableEvent>(),
        "isCancelled",
        "()Z"
    )
}

public fun InsnBuilder.returnIfEventCancelled(insn: InsnList = asm { _return }) {
    val end = LabelNode()

    isEventCancelled()
    ifeq(end)

    +insn

    +end
    f_same()
}