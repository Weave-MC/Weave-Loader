package net.weavemc.api.event

import net.minecraft.client.Minecraft
import net.minecraft.world.World

/**
 * Non-cancellable event, split into [WorldEvent.Load] and [WorldEvent.Unload].
 *
 * Event call in the event of loading, or unloading a world.
 */
public sealed class WorldEvent(public val world: World) : Event() {
    /**
     * Called in correspondence with [Minecraft.loadWorld]
     * if [net.minecraft.client.multiplayer.WorldClient] is not null.
     * Therefore, [WorldEvent.Load] is called by the [EventBus] only in the event
     * that a client loads a new world. Whether that be a server connection,
     * or a singleplayer world.
     */
    public class Load(world: World) : WorldEvent(world)

    /**
     * Called in correspondence with [Minecraft.loadWorld]
     * if [Minecraft.theWorld] is not null.
     * Therefore, [WorldEvent.Unload] is called by the [EventBus] only in the event
     * that a client unloads a world. Whether that be a server disconnection,
     * or a singleplayer world.
     */
    public class Unload(world: World) : WorldEvent(world)
}