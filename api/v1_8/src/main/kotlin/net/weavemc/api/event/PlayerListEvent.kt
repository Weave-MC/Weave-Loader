package net.weavemc.api.event

import net.minecraft.network.play.server.S38PacketPlayerListItem

/**
 * This event is called when a player is added or removed from the player list.
 *
 * This event is split into [PlayerListEvent.Add] and [PlayerListEvent.Remove].
 *
 * @property playerData The Player Data of the player being added/removed.
 */
sealed class PlayerListEvent(val playerData: S38PacketPlayerListItem.AddPlayerData) : Event() {
    /**
     * This is called when a player is added to the player list.
     */
    class Add(playerData: S38PacketPlayerListItem.AddPlayerData) : PlayerListEvent(playerData)

    /**
     * This is called when a player is removed from the player list.
     */
    class Remove(playerData: S38PacketPlayerListItem.AddPlayerData) : PlayerListEvent(playerData)
}