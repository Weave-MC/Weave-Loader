package net.weavemc.loader.api.event;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The event bus handles events and event listeners.
 * @see Event
 * @see SubscribeEvent @SubscribeEvent
 */
@UtilityClass
public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> map = new ConcurrentHashMap<>();

    /**
     * Subscribe an object to the event bus, turning methods
     * defined in it into listeners using @SubscribeEvent
     * @see SubscribeEvent @SubscribeEvent
     * @param obj The object to subscribe.
     */
    public void subscribe(Object obj) {
        for (Method method : obj.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SubscribeEvent.class) && method.getParameterCount() == 1) {
                getListeners(method.getParameterTypes()[0]).add(new ReflectEventConsumer(obj, method));
            }
        }
    }

    /**
     * Subscribe a listener to the event bus.
     * @param event The class of the event to subscribe to.
     * @param handler The Consumer to handle that event.
     * @param <T> The type of the event to listen for.
     */
    public<T extends Event> void subscribe(Class<T> event, Consumer<T> handler) {
        getListeners(event).add(handler);
    }

    /**
     * Call an event for all the listeners listening for it.
     * @param event The event to call.
     * @param <T> The type of the event to call.
     */
    public<T extends Event> void callEvent(T event) {
        for(Class<?> c = event.getClass(); c != Object.class; c = c.getSuperclass()) {
            //noinspection unchecked
            getListeners(c).forEach(l -> ((Consumer<T>)l).accept(event));
        }
    }

    /**
     * Unsubscribe a listener from the event bus.
     * @param consumer The Consumer to unsubscribe.
     */
    public void unsubscribe(Consumer<? extends Event> consumer) {
        for(List<Consumer<?>> list : map.values()) {
            list.removeIf(c -> c == consumer);
        }
    }

    /**
     * Unsubscribe an object from the event bus, which unsubscribes
     * all of its listeners.
     * @param obj The object to unsubscribe.
     */
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
