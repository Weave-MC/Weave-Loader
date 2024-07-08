package net.weavemc.loader.api.event

/**
 * This is the base class for all events provided by the Weave Loader.
 */
public abstract class Event

/**
 * This is the base class for all *cancellable* events provided by the
 * Weave Loader, extending [Event].
 */
public abstract class CancellableEvent : Event() {
    /**
     * This field defines whether the event is cancelled or not. Any mod can cancel and
     * un-cancel an event. What an event does when cancelled is event-specific, and noted in
     * that event's documentation.
     */
    @get:JvmName("isCancelled")
    public var cancelled: Boolean = false
}
