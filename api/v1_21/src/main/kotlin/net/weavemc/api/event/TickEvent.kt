package net.weavemc.api.event

public sealed class TickEvent : Event() {
    /**
     * Pre Tick Events are called at the start of a tick.
     */
    public object Pre : TickEvent()

    /**
     * Post Tick Events are called at the end of a tick.
     */
    public object Post: TickEvent()
}