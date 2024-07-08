package net.weavemc.loader.api.bytecode

import net.weavemc.loader.api.event.Event
import net.weavemc.loader.api.event.EventBus
import net.weavemc.internals.InsnBuilder
import net.weavemc.internals.internalNameOf

/**
 * Pops an [Event] off the stack and posts the [Event] to the [EventBus]
 */
public fun InsnBuilder.postEvent() {
    invokestatic(
        internalNameOf<EventBus>(),
        "postEvent",
        "(L${internalNameOf<Event>()};)V"
    )
}