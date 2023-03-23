package club.maxstats.weave.loader.api.command;

import lombok.AllArgsConstructor;
import java.util.Arrays;

@AllArgsConstructor
public abstract class Command {
    public final String name;

    public String[] getAliases() {
        return new String[] {};
    }

    public abstract void handle(String[] args);


    boolean matches(String s) {
        return name.equalsIgnoreCase(s) || Arrays.stream(getAliases()).anyMatch(alias -> alias.equalsIgnoreCase(s));
    }
}
