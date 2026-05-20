package net.weavemc.api.event

sealed class TickEvent : Event() {
    /**
     * Pre Tick Events are called at the start of a tick.
     */
    object Pre : TickEvent()

    /**
     * Post Tick Events are called at the end of a tick.
     */
    object Post: TickEvent()
}