package net.weavemc.api.event

import net.minecraft.network.play.server.S38PacketPlayerListItem

/**
 * This event is called when a player is added or removed from the player list.
 *
 * This event is split into [PlayerListEvent.Add] and [PlayerListEvent.Remove].
 */
sealed class PlayerListEvent(packet: S38PacketPlayerListItem) : Event() {
    val name: String = packet.func_149122_c()

    val ping: Int = packet.func_149120_e()

    /**
     * This is called when a player is added to the player list.
     */
    class Add(packet: S38PacketPlayerListItem) : PlayerListEvent(packet)

    /**
     * This is called when a player is removed from the player list.
     */
    class Remove(packet: S38PacketPlayerListItem) : PlayerListEvent(packet)

}