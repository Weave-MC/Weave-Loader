package net.weavemc.api.command

import net.minecraft.util.BlockPos

/**
 * The base Command class for commands using the Weave command system.
 *
 * @see [net.weavemc.api.command.CommandBus]
 *
 * @param name The name of the command.
 * @param aliases Alternative names that can be used to trigger this command.
 * @param exclusiveSuggestions When <code>true</code>, prevents the client from requesting server-side suggestions if [getSuggestions] returns a non-null result. If <code>false</code>, the server's suggestions will be merged with the local results and ordered using the [orderSuggestions] method.
 * @param showInRoot If <code>true</code>, this command's name and aliases will be included in the suggestion list when the player requests completions for an empty command (e.g. typing only '/').
 * @param caseSensitive Determines whether the command name and aliases should match the input exactly or ignore casing.
 */
abstract class Command @JvmOverloads constructor(
    val name: String,
    vararg aliases: String = emptyArray(),
    var exclusiveSuggestions: Boolean = false,
    var showInRoot: Boolean = true,
    var caseSensitive: Boolean = false
) : Comparable<Command> {
    private val names: List<String> = listOf(name, *aliases)

    /**
     * Performs the command's action with the specified arguments.
     *
     * @param args Array of strings separated by whitespace following the initial command name i.e. <code>/command arg arg arg</code>.
     */
    abstract fun execute(args: Array<String>)

    /**
     * Provides a list of suggestions for tab-completion based on the current arguments and the block the player is currently looking at.
     *
     * @param args The arguments following the command name (the name itself is excluded).
     * @param targetPos The block position the sender is currently looking at. May be <code>null</code> if the sender is not looking at a block.
     * @return An array of potential completion strings, or <code>null</code> if no suggestions are available.
     */
    open fun getSuggestions(args: Array<String>, targetPos: BlockPos?): Array<String>? = null

    /**
     * Merges and reorders suggestions when [exclusiveSuggestions] is false.
     *
     * This method is called when both local suggestions and server-side suggestions are available.
     * By default, it combines both arrays and removes duplicates, preserving the original order of local suggestions followed by server suggestions.
     *
     * This method is ignored during root-level completion (when the player is tab-completing the command name itself).
     * In that scenario, all available command names and aliases are merged and sorted alphabetically.
     *
     * @param ownSuggestions The suggestions returned by this command's [getSuggestions] method.
     * @param serverSuggestions The suggestions received from the server.
     * @return A consolidated and ordered array of strings to be displayed in the chat GUI.
     */
    open fun orderSuggestions(ownSuggestions: Array<String>, serverSuggestions: Array<String>): Array<String> =
        (serverSuggestions + ownSuggestions).distinctLast()

    /**
     * Checks whether the provided message matches the command name or any of its aliases.
     *
     * @param message The input string to check against the command's names.
     * @return <code>true</code> if the message matches the name or an alias; <code>false</code> otherwise.
     */
    open fun matches(message: String): Boolean =
        names.any { message.equals(it, !caseSensitive) }

    /**
     * Retrieves all names and aliases of this command that start with the provided message.
     *
     * @param message The input string to check; this may be a partial command name or empty.
     * @return A list of matching names and aliases that begin with the message.
     */
    open fun matching(message: String): List<String> =
        names.filter { it.startsWith(message, !caseSensitive) }

    override fun compareTo(other: Command): Int =
        name.compareTo(other.name, ignoreCase = !caseSensitive)

    protected fun Array<String>.distinctLast(): Array<String> {
        if (this.size <= 1) {
            return this
        }

        val seen = HashSet<String>(this.size)
        val result = ArrayList<String>(this.size)

        for (i in this.indices.reversed()) {
            val element = this[i]
            if (seen.add(element)) {
                result.add(element)
            }
        }

        return result.reversed().toTypedArray()
    }
}