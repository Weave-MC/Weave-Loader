package club.maxstats.weave.loader.api

import club.maxstats.weave.loader.api.event.ChatSentEvent
import club.maxstats.weave.loader.api.event.EventBus
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent

private val whitespaceRegex = """\s+""".toRegex()

object CommandBus {
    private val commands = mutableListOf<Command>()

    fun register(command: Command) {
        commands += command
    }

    fun register(name: String, builder: CommandBuilder.() -> Unit) = register(command(name, builder))

    inline fun registerSimple(name: String, crossinline handler: CommandContext.() -> Unit) =
        register(simpleCommand(name, handler))

    internal fun registerCommandHook() = EventBus.subscribe<ChatSentEvent> { e ->
        if (!e.message.startsWith('/')) return@subscribe

        val commandPart = e.message.drop(1)
        val partialArgs = commandPart.split(whitespaceRegex)
        val command = commands.find { it.matches(partialArgs.first()) } ?: return@subscribe
        e.cancelled = true
        command.handle(partialArgs.drop(1))
    }
}

private fun Command.matches(name: String) = this.name.equals(name, ignoreCase = true) || name in aliases

abstract class Command {
    abstract val name: String
    open val aliases = listOf<String>()
    open val subCommands = mutableListOf<Command>()
    open val usage get() = if (subCommands.isEmpty()) "no usage" else subCommands.joinToString(
        separator = "|",
        prefix = "<",
        postfix = ">"
    ) { it.name }

    fun registerSubCommand(command: Command) {
        subCommands += command
    }

    open fun handle(args: List<String>) {
        when {
            args.isEmpty() -> return printUsage()
            else -> (subCommands.find { it.matches(args.first()) } ?: return printUsage()).handle(args.drop(1))
        }
    }

    fun printUsage() {
        val component = component("usage: $name $usage") { color = EnumChatFormatting.RED }
        Minecraft.getMinecraft().thePlayer.addChatMessage(component)
    }
}

inline fun simpleCommand(name: String, crossinline handler: CommandContext.() -> Unit) = object : Command() {
    override val name = name
    override fun handle(args: List<String>) {
        handler(CommandContext(args))
    }
}

inline fun command(name: String, builder: CommandBuilder.() -> Unit) = CommandBuilder(name).also(builder).toCommand()

class CommandBuilder(private val name: String) {
    var usage: String? = null
    private var handler: (CommandContext.() -> Unit)? = null
    private val aliases = mutableListOf<String>()
    private val subCommands = mutableListOf<Command>()

    fun alias(vararg alias: String) {
        aliases += alias
    }

    fun subCommand(name: String, builder: CommandBuilder.() -> Unit) {
        subCommands += command(name, builder)
    }

    fun simpleSubCommand(name: String, handler: CommandContext.() -> Unit) {
        subCommands += simpleCommand(name, handler)
    }

    fun handle(handler: CommandContext.() -> Unit) {
        this.handler = handler
    }

    fun toCommand() = object : Command() {
        override val name = this@CommandBuilder.name
        override val usage = this@CommandBuilder.usage ?: super.usage
        override val aliases = this@CommandBuilder.aliases
        override val subCommands = this@CommandBuilder.subCommands

        override fun handle(args: List<String>) {
            (handler ?: return super.handle(args))(CommandContext(args))
        }
    }
}

class CommandContext(val args: List<String>) {
    val player get() = Minecraft.getMinecraft().thePlayer

    fun message(message: String) = player.addChatMessage(component(message))
    fun message(component: IChatComponent) = player.addChatMessage(component)
    fun errorMessage(message: String) = message(component(message) { color = EnumChatFormatting.RED })
}

inline fun component(text: String, builder: ChatStyle.() -> Unit = {}) =
    ChatComponentText(text).apply { chatStyle.builder() }