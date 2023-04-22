package net.weavemc.loader.api.event.client

import net.minecraft.util.IChatComponent
import net.weavemc.loader.api.event.CancellableEvent

public sealed class ChatEvent : CancellableEvent() {
    public class Received(public val message: IChatComponent) : ChatEvent()

    public class Sent(public val message: String) : ChatEvent()
}
