package net.weavemc.api.event

import net.minecraft.client.gui.hud.MessageIndicator
import net.minecraft.network.message.ArgumentSignatureDataMap
import net.minecraft.network.message.MessageSignatureData
import net.minecraft.text.Text
import java.time.Instant

public sealed class ChatEvent : CancellableEvent() {
    /**
     * This cancellable event is called when your client receives a chat message from the server.
     *
     * @property message The message being received.
     * @property signature The cryptographic signature used to verify sender authenticity, or `null` if unsigned.
     * @property indicator The visual badge rendered next to the message in the chat HUD, or `null` if none.
     */
    public class Received(
        public var message: Text,
        public val signature: MessageSignatureData?,
        public var indicator: MessageIndicator?
    ) : ChatEvent()

    /**
     * Called when the client attempts to send a chat message or command to the server.
     *
     * Cancelling this event prevents the packet from being dispatched to the network.
     *
     * @property message The message or command string being sent.
     * @property timestamp The creation timestamp generated for this packet.
     */
    public sealed class Sent @JvmOverloads constructor(
        public var message: String,
        public val timestamp: Instant = Instant.now()
    ) : ChatEvent() {
        /**
         * Called when a standard chat message is being sent to the server.
         *
         * @property message The raw chat text (e.g. `"Hello world"`).
         * @property signature The cryptographic signature generated for this message, or `null` if unsigned. Can be modified or set to `null` to strip signatures.
         */
        public class Chat @JvmOverloads constructor(
            message: String,
            public var signature: MessageSignatureData?,
            timestamp: Instant = Instant.now()
        ) : Sent(message, timestamp)

        /**
         * Called when a slash command is executed by the client.
         *
         * @property message The command execution string **without** the leading `/` (e.g. `"tp PlayerA PlayerB"` or `"msg PlayerA hello"`).
         * @property isSigned Whether this command contains chat-like arguments that require cryptographic signatures.
         * @property argumentSignatures The signatures mapped to specific command arguments. Can be modified or cleared.
         */
        public class Command @JvmOverloads constructor(
            message: String,
            public val isSigned: Boolean,
            public var argumentSignatures: ArgumentSignatureDataMap = ArgumentSignatureDataMap.EMPTY,
            timestamp: Instant = Instant.now()
        ) : Sent(message, timestamp)
    }
}