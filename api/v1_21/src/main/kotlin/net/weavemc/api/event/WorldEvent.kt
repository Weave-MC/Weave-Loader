package net.weavemc.api.event

import net.minecraft.world.World

/**
 * Non-cancellable event, split into [WorldEvent.Load] and [WorldEvent.Unload].
 *
 * Fired during the loading or unloading phase of a world.
 */
public sealed class WorldEvent(public val world: World) : Event() {
    /**
     * Called when [net.minecraft.client.MinecraftClient.setWorld] is executed
     * and a non-null [net.minecraft.client.world.ClientWorld] is passed in.
     */
    public class Load(world: World) : WorldEvent(world)

    /**
     * Called when [net.minecraft.client.MinecraftClient.setWorld] is executed
     * while [net.minecraft.client.MinecraftClient.world] is currently non-null.
     */
    public class Unload(world: World) : WorldEvent(world)
}