package net.weavemc.loader.api.command;

import lombok.experimental.UtilityClass;
import net.weavemc.loader.api.event.ChatSentEvent;
import net.weavemc.loader.api.event.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The Command Bus manages commands by Weave mods.
 */
@UtilityClass
public class CommandBus {
    private final List<Command> commands = new ArrayList<>();

    /**
     * While commands can be registered at any time and still work, outside rare cases,
     * you should register them during your ModInitializer's `preInit()`.
     * @param command The command instance to register.
     */
    public void register(Command command) {
        commands.add(command);
    }

    static {
        EventBus.subscribe(ChatSentEvent.class, e -> {
            if(!e.getMessage().startsWith("/")) return;

            String[] split = e.getMessage().substring(1).split("\\s+");

            Iterator<Command> matching = commands.stream().filter(c -> c.matches(split[0])).iterator();
            if(!matching.hasNext()) return;

            e.setCancelled(true);
            String[] args = Arrays.copyOfRange(split, 1, split.length);
            matching.forEachRemaining(c -> c.handle(args));
        });
    }
}
