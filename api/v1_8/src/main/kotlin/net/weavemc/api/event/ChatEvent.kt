package net.weavemc.api.event

import net.minecraft.util.IChatComponent

sealed class ChatEvent : CancellableEvent() {
    /**
     * This cancellable event is called when your client receives a chat message from the server.
     *
     * @property message The message being received, in the form of a [Chat Component][IChatComponent].
     */
    class Received(val message: IChatComponent) : ChatEvent()

    /**
     * This cancellable event is called when your client sends a chat message to the server.
     *
     * If cancelled, the message will not be sent, but still added to your chat history.
     * This can be useful for making your own command system.
     *
     * @property message The message that is going to be sent.
     */
    class Sent(val message: String) : ChatEvent()
}