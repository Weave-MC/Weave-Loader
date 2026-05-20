package net.weavemc.api.event

import net.minecraft.client.Minecraft
import net.minecraft.network.Packet

/**
 * Cancellable event, split into [PacketEvent.Send] and [PacketEvent.Receive].
 *
 * Called in the event of a packet being sent or received to the client via [Minecraft.myNetworkManager]
 *
 * @property packet The packet being processed.
 */
sealed class PacketEvent(var packet: Packet<*>) : CancellableEvent() {
    /**
     * Called in correspondence with [net.minecraft.network.NetworkManager.scheduleOutboundPacket],
     * which is called in the event that the client sends a packet to the server.
     *
     * When cancelled, packets are not sent to the server.
     *
     * @param packet The packet being sent.
     */
    class Send(packet: Packet<*>) : PacketEvent(packet)

    /**
     * Called in correspondence with [net.minecraft.network.NetworkManager.channelRead0],
     * which is called in the event that the client receives a packet from the server.
     *
     * When cancelled, packets are not processed by the client.
     *
     * @param packet The packet being received.
     */
    class Receive(packet: Packet<*>) : PacketEvent(packet)
}