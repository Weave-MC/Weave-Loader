package net.weavemc.api.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientCommandSource
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import net.weavemc.api.event.ChatEvent
import net.weavemc.api.event.CommandEvent
import net.weavemc.api.event.EventBus
import net.weavemc.api.event.SubscribeEvent

/**
 * Manages client-side Brigadier commands registered by mods.
 */
public object CommandManager {
    init {
        EventBus.subscribe(Listener.ChatListener)
    }

    /**
     * The client-side command dispatcher.
     */
    @JvmStatic
    public val dispatcher: CommandDispatcher<ClientCommandSource> = CommandDispatcher()

    /**
     * Returns a valid [ClientCommandSource] bound to the current client network handler.
     */
    @JvmStatic
    public val commandSource: ClientCommandSource?
        get() = MinecraftClient.getInstance()?.networkHandler?.commandSource

    /**
     * Helper to create a literal command node.
     */
    @JvmStatic
    public fun literal(name: String): LiteralArgumentBuilder<ClientCommandSource> =
        LiteralArgumentBuilder.literal(name)

    /**
     * Helper to create an argument command node.
     */
    @JvmStatic
    public fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<ClientCommandSource, T> =
        RequiredArgumentBuilder.argument(name, type)

    /**
     * Prints a Brigadier command exception nicely into the client chat HUD.
     */
    @JvmStatic
    public fun CommandSyntaxException.sendErrorToChat() {
        val errorText = Texts.toText(rawMessage).copy().formatted(Formatting.byName("RED"))
        MinecraftClient.getInstance()?.inGameHud?.chatHud?.addMessage(errorText)
    }

    /**
     * Merges all registered mod commands into [targetDispatcher].
     */
    @JvmStatic
    public fun mergeInto(targetDispatcher: CommandDispatcher<ClientCommandSource>) {
        for (node in dispatcher.root.children) {
            targetDispatcher.root.addChild(node as CommandNode<ClientCommandSource>)
        }
    }

    private object Listener {
        object ChatListener {
            @SubscribeEvent
            fun onChatEventSentCommand(event: ChatEvent.Sent.Command) {
                val parseResults = dispatcher.parse(event.message, commandSource ?: return)
                if (parseResults.reader.canRead() && parseResults.exceptions.isEmpty()) {
                    return
                }

                try {
                    dispatcher.execute(parseResults)
                } catch (e: CommandSyntaxException) {
                    e.sendErrorToChat()
                }

                event.cancelled = true
            }

            @SubscribeEvent
            fun onCommandEventRegister(event: CommandEvent.Register) {
                mergeInto(event.dispatcher)
            }
        }
    }
}