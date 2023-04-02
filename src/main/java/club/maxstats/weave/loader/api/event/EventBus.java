package club.maxstats.weave.loader.api.event;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@UtilityClass
public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> map = new ConcurrentHashMap<>();

    public void subscribe(Object obj) {
        for (Method method : obj.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SubscribeEvent.class) && method.getParameterCount() == 1) {
                getListeners(method.getParameterTypes()[0]).add(new ReflectEventConsumer(obj, method));
            }
        }
    }

    public<T extends Event> void subscribe(Class<T> event, Consumer<T> handler) {
        getListeners(event).add(handler);
    }

    public<T extends Event> void callEvent(T event) {
        for(Class<?> c = event.getClass(); !Modifier.isAbstract(c.getModifiers());c = c.getSuperclass()) {
            //noinspection unchecked
            getListeners(c).forEach(l -> ((Consumer<T>)l).accept(event));
        }
    }

    public void unsubscribe(Consumer<? extends Event> consumer) {
        for(List<Consumer<?>> list : map.values()) {
            list.removeIf(c -> c == consumer);
        }
    }

    public void unsubscribe(Object obj) {
        for(List<Consumer<?>> list : map.values()) {
            list.removeIf(c -> c instanceof ReflectEventConsumer && ((ReflectEventConsumer)c).obj == obj);
        }
    }

    @NotNull
    private List<Consumer<?>> getListeners(Class<?> event) {
        return map.computeIfAbsent(event, e -> new CopyOnWriteArrayList<>());
    }

    @AllArgsConstructor
    private class ReflectEventConsumer implements Consumer<Event> {
        private final Object obj;
        private final Method method;

        @Override
        @SneakyThrows
        public void accept(Event event) {
            method.invoke(obj, event);
        }
    }
}
