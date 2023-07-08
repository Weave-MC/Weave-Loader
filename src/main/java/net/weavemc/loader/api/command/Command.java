package net.weavemc.loader.api.command;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * The base Command class for commands using the Weave command system.
 *
 * @see CommandBus
 */
public abstract class Command {

    public final String   name;
    public final String[] aliases;

    /**
     * @param name    The name of the command.
     * @param aliases Zero or more substitutes for the command's name.
     */
    public Command(String name, String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    /**
     * 'Handles' behavior on command execution.
     *
     * @param args Array of strings seperated by whitespace following the initial command name.
     *             i.e. {@code /command arg arg arg}.
     */
    public abstract void handle(@NotNull String[] args);

    boolean matches(String s) {
        return name.equalsIgnoreCase(s) || Arrays.stream(aliases).anyMatch(alias -> alias.equalsIgnoreCase(s));
    }

}
