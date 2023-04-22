package net.weavemc.loader.api.event.client

import net.weavemc.loader.api.event.Event

public sealed class TickEvent : Event() {
    public object Pre : TickEvent()

    public object Post: TickEvent()
}

public sealed class PlayerTickEvent : Event() {
    public object Pre : PlayerTickEvent()

    public object Post : PlayerTickEvent()
}
