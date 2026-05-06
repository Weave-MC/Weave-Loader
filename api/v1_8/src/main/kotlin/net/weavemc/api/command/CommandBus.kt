package net.weavemc.api.command

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.network.play.client.C14PacketTabComplete
import net.minecraft.network.play.server.S3APacketTabComplete
import net.weavemc.api.ChatSentEvent
import net.weavemc.api.PacketEvent
import net.weavemc.api.event.EventBus
import net.weavemc.api.event.SubscribeEvent

/**
 * The Command Bus manages commands by Weave mods.
 *
 * This implementation differs from Weave 0.x in that only the first matched command is executed.
 * If multiple commands share a name or aliases, the command registered earliest takes precedence.
 */
object CommandBus {
    private val commands = mutableListOf<Command>()

    private val WHITESPACE_REGEX = Regex("\\s+")

    private fun String.split(): List<String> = split(WHITESPACE_REGEX)

    init {
        arrayOf(
            ChatListener,
            TabCompletionListener
        ).forEach(EventBus::subscribe)
    }

    /**
     * Commands can be registered at any time during the client lifecycle.
     *
     * @param command The command instance to register.
     */
    @JvmStatic
    fun register(command: Command) {
        commands.add(command)
    }

    private object ChatListener {
        @SubscribeEvent
        fun onChatSentEvent(event: ChatSentEvent) {
            val message = event.message.trim()

            if (message[0] != '/') {
                return
            }

            val args = message.drop(1).split()

            commands
                .find { it.matches(args[0]) }
                ?.execute(args.toTypedArray())
        }
    }

    private object TabCompletionListener {
        @Volatile
        var lastCommandAndSuggestions: Pair<Command?, Array<String>>? = null

        private val guiChat: GuiChat?
            get() = Minecraft.getMinecraft().currentScreen as? GuiChat

        @SubscribeEvent
        fun onPacketSend(event: PacketEvent.Send) {
            val packet = event.packet
            if (packet !is C14PacketTabComplete) {
                return
            }

            val message = packet.message.trim()
            val commandArgs = message.split()

            if (commandArgs.size == 1 && message.startsWith('/')) {
                val suggestions = commands
                    .filter(Command::showInRoot)
                    .flatMap { it.matching(message) }
                    .toTypedArray()

                lastCommandAndSuggestions = null to suggestions
            } else {
                val targetBlock = packet.targetBlock

                for (command in commands) {
                    val suggestions = command.getSuggestions(commandArgs.drop(1).toTypedArray(), targetBlock) ?: continue

                    if (command.exclusiveSuggestions) {
                        guiChat?.run {
                            lastCommandAndSuggestions = null

                            onAutocompleteResponse(suggestions)

                            event.cancelled = true
                        }
                    } else {
                        lastCommandAndSuggestions = command to suggestions
                    }

                    break
                }
            }
        }

        @SubscribeEvent
        fun onPacketReceive(event: PacketEvent.Receive) {
            val packet = event.packet
            if (packet !is S3APacketTabComplete) {
                return
            }

            val (lastCommand, lastSuggestions) = lastCommandAndSuggestions ?: return
            val serverSuggestions = packet.matches

            lastCommandAndSuggestions = null

            val orderedSuggestions = lastCommand?.orderSuggestions(lastSuggestions, serverSuggestions)
                ?: (lastSuggestions + serverSuggestions).distinct().sorted().toTypedArray()

            packet.matches = orderedSuggestions
        }
    }
}