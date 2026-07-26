package net.weavemc.api.event

import net.minecraft.network.packet.Packet

/**
 * Cancellable event, split into [PacketEvent.Send] and [PacketEvent.Receive].
 *
 * Called when a packet is sent or received by the client via [net.minecraft.network.ClientConnection].
 *
 * @property packet The packet being processed.
 */
public sealed class PacketEvent(public var packet: Packet<*>) : CancellableEvent() {
    /**
     * Called when the client attempts to send a packet to the server.
     *
     * When cancelled, the packet is suppressed and not written to the Netty channel.
     */
    public class Send(packet: Packet<*>) : PacketEvent(packet)

    /**
     * Called when the client receives a packet from the server.
     *
     * When cancelled, the packet is dropped before being passed to the client packet listener.
     */
    public class Receive(packet: Packet<*>) : PacketEvent(packet)
}