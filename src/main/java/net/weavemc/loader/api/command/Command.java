package net.weavemc.loader.api.command;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * The base Command class for commands using the Weave command system.
 *
 * @see CommandBus
 */
public abstract class Command {
    public final String name;
    public final String[] aliases;

    /**
     * The constructor for commands.
     * @param name The name of the command.
     * @param aliases The other aliases of the command, which can be used
     *                to call the command without using its name. Usually just
     *                a shorter version of the command's name.
     */
    public Command(String name, String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    /**
     * This method is called when the command is executed.
     * @param args An array of everything inputted after the command's name/alias,
     *             split by space.
     */
    public abstract void handle(@NotNull String[] args);

    boolean matches(String s) {
        return name.equalsIgnoreCase(s) || Arrays.stream(aliases).anyMatch(alias -> alias.equalsIgnoreCase(s));
    }
}
