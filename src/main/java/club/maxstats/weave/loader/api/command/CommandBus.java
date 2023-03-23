package club.maxstats.weave.loader.api.command;

import club.maxstats.weave.loader.api.event.ChatSentEvent;
import club.maxstats.weave.loader.api.event.EventBus;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class CommandBus {
    private final List<Command> commands = new ArrayList<>();

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
