package club.maxstats.weave.loader.api

import club.maxstats.weave.loader.api.event.ChatSentEvent
import club.maxstats.weave.loader.api.event.EventBus
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText

private val whitespaceRegex = """\s+""".toRegex()

public object CommandBus {

    private val commands = mutableListOf<Command>()

    public fun register(command: Command) {
        commands += command
    }

    public fun register(name: String, builder: CommandBuilder.() -> Unit) {
        register(command(name, builder))
    }

    public inline fun registerSimple(name: String, crossinline handler: CommandContext.() -> Unit) {
        register(simpleCommand(name, handler))
    }

    internal fun init() = EventBus.subscribe<ChatSentEvent> { e ->
        if (!e.message.startsWith('/')) return@subscribe

        val commandPart = e.message.drop(1)
        val partialArgs = commandPart.split(whitespaceRegex)
        val command = commands.find { it.matches(partialArgs.first()) } ?: return@subscribe
        e.cancelled = true
        command.handle(partialArgs.drop(1))
    }

}

private fun Command.matches(name: String) =
    (this.aliases + this.name).any { it.equals(name, ignoreCase = true) }

public abstract class Command {

    public abstract val name: String
    public open val aliases: List<String> = listOf()
    public open val subCommands: List<Command> = listOf()
    public open val usage: String
        get() = if (subCommands.isEmpty()) "no usage" else subCommands.joinToString(
            separator = "|",
            prefix = "<",
            postfix = ">"
        ) { it.name }

    public open fun handle(args: List<String>) {
        when {
            args.isEmpty() -> return printUsage()
            else -> (subCommands.find { it.matches(args.first()) } ?: return printUsage()).handle(args.drop(1))
        }
    }

    private fun printUsage() {
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            ChatComponentText("§4usage: $name $usage")
        )
    }

}

public inline fun simpleCommand(name: String, crossinline handler: CommandContext.() -> Unit): Command =
    object : Command() {
        override val name = name
        override fun handle(args: List<String>) {
            handler(CommandContext(args))
        }
    }

public inline fun command(name: String, builder: CommandBuilder.() -> Unit): Command =
    CommandBuilder(name).also(builder).toCommand()

public class CommandBuilder(private val name: String) {

    public var usage: String? = null
    private var handler: (CommandContext.() -> Unit)? = null
    private val aliases = mutableListOf<String>()
    private val subCommands = mutableListOf<Command>()

    public fun alias(alias: String) {
        aliases += alias
    }

    public fun subCommand(name: String, builder: CommandBuilder.() -> Unit) {
        subCommands += command(name, builder)
    }

    public fun simpleSubCommand(name: String, handler: CommandContext.() -> Unit) {
        subCommands += simpleCommand(name, handler)
    }

    public fun handle(handler: CommandContext.() -> Unit) {
        this.handler = handler
    }

    public fun toCommand(): Command = object : Command() {
        override val name = this@CommandBuilder.name
        override val usage = this@CommandBuilder.usage ?: super.usage
        override val aliases = this@CommandBuilder.aliases
        override val subCommands = this@CommandBuilder.subCommands

        override fun handle(args: List<String>) {
            (handler ?: return super.handle(args))(CommandContext(args))
        }
    }

}

public class CommandContext(public val args: List<String>)
