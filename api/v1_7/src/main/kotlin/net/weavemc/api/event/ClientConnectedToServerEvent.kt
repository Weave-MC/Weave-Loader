package net.weavemc.api.event

import net.minecraft.network.NetworkManager

/**
 * Non-cancellable event, called at the tail of [net.minecraft.client.network.NetHandlerLoginClient.handleLoginSuccess], therefor after a client has connected to a server
 *
 * @property manager The Network Manager
 */
class ClientConnectedToServerEvent(val manager: NetworkManager) : Event() {
    val isLocal: Boolean
        get() = manager.isLocalChannel
}