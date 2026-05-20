package net.weavemc.api.event

import net.minecraft.client.Minecraft

/**
 * Non-cancellable event,
 * Called in correspondence with [Minecraft.shutdownMinecraftApplet].
 * Therefore, [ShutdownEvent] is called by the [EventBus] only in the event that
 * the client is being shut down.
 */
object ShutdownEvent : Event()