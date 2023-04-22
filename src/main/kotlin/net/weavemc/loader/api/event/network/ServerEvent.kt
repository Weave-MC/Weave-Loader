package net.weavemc.loader.api.event.network

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ServerData
import net.weavemc.loader.api.event.Event

public class ServerConnectEvent(
    public val ip: String,
    public val port: Int,
) : Event() {
    public val serverData: ServerData = Minecraft.getMinecraft().currentServerData
}
