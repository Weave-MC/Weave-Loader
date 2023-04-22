package net.weavemc.loader.api.event.network

import net.minecraft.network.Packet
import net.weavemc.loader.api.event.CancellableEvent

public sealed class PacketEvent(public val packet: Packet<*>) : CancellableEvent() {
    public class Send(packet: Packet<*>) : PacketEvent(packet)

    public class Receive(packet: Packet<*>) : PacketEvent(packet)
}
