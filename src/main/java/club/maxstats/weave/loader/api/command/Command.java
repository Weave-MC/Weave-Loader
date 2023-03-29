package club.maxstats.weave.loader.api.command;

import java.util.Arrays;

public abstract class Command {
    public final String name;
    public final String[] aliases;

    public Command(String name, String... aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public abstract void handle(String[] args);

    boolean matches(String s) {
        return name.equalsIgnoreCase(s) || Arrays.stream(aliases).anyMatch(alias -> alias.equalsIgnoreCase(s));
    }
}
