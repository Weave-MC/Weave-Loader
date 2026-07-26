package net.weavemc.api.event

/**
 * Non-cancellable event,
 * Called in correspondence with [net.minecraft.client.MinecraftClient.stop].
 * Therefore, [ShutdownEvent] is called by the [EventBus] only in the event that
 * the client is being shut down.
 */
public object ShutdownEvent : Event()