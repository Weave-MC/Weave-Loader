package net.weavemc.api.event

import net.minecraft.network.ClientConnection
import net.minecraft.network.DisconnectionInfo
import net.minecraft.text.Text
import java.net.URI
import java.nio.file.Path
import java.util.*

/**
 * Base class for all server connection lifecycle events.
 *
 * @property connection The underlying client network connection manager.
 */
public sealed class ServerConnectionEvent(
    public val connection: ClientConnection
) : Event() {
    /**
     * Whether the connection is to a local singleplayer/integrated server,
     * as opposed to a remote multiplayer server.
     */
    public val isLocal: Boolean get() = connection.isLocal

    /**
     * Non-cancellable event, called at the tail of [net.minecraft.client.network.ClientLoginNetworkHandler.onSuccess]
     * after a client has successfully authenticated and connected to a server/integrated world.
     *
     * @property connection The network connection manager.
     */
    public class Connect(
        connection: ClientConnection
    ) : ServerConnectionEvent(connection)

    /**
     * Fired when the client network handler is disconnected from the server/integrated world.
     *
     * @property connection The network connection manager.
     * @property info The raw disconnection details provided by Minecraft.
     */
    public class Disconnect(
        connection: ClientConnection,
        public val info: DisconnectionInfo
    ) : ServerConnectionEvent(connection) {
        /**
         * The text component explaining why the connection was lost.
         */
        public val reason: Text get() = info.comp_2853

        /**
         * Optional path to a local report/crash log file generated for this disconnect.
         */
        public val report: Optional<Path?>? get() = info.comp_2854

        /**
         * Optional web link for reporting bugs or accessing server support.
         */
        public val bugReportLink: Optional<URI?>? get() = info.comp_2855
    }
}