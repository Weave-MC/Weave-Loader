package net.weavemc.loader.api.event

public abstract class Event

public abstract class CancellableEvent : Event() {
    @get:JvmName("isCancelled")
    public var cancelled: Boolean = false
}
