package net.weavemc.api.event

import net.minecraft.util.text.ITextComponent

public sealed class ChatEvent : CancellableEvent() {
    /**
     * This cancellable event is called when your client receives a chat message from the server.
     *
     * @property message The message being received, in the form of a [Chat Component][IChatComponent].
     */
    public class Received(public val message: ITextComponent) : ChatEvent()

    /**
     * This cancellable event is called when your client sends a chat message to the server.
     *
     * If cancelled, the message will not be sent, but still added to your chat history.
     * This can be useful for making your own command system.
     *
     * @property message The message that is going to be sent.
     */
    public class Sent(public val message: String) : ChatEvent()
}