package net.weavemc.api.bytecode

import net.weavemc.api.event.Event
import net.weavemc.api.event.EventBus
import net.weavemc.internals.InsnBuilder
import net.weavemc.internals.internalNameOf

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