package net.weavemc.api.event

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.client.network.ClientCommandSource
import net.minecraft.command.CommandRegistryAccess

public sealed class CommandEvent : Event() {
    /**
     * Called when client commands are ready to be registered to Brigadier.
     *
     * @property dispatcher The Brigadier dispatcher used to register commands.
     * @property registryAccess Access to client registry data.
     */
    public class Register(
        public val dispatcher: CommandDispatcher<ClientCommandSource>,
        public val registryAccess: CommandRegistryAccess
    ) : CommandEvent()
}