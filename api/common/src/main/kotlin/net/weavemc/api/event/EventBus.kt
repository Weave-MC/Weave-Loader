package net.weavemc.api.event

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * The event bus handles events and event listeners.
 *
 * @see Event
 *
 * @see SubscribeEvent
 */
@Suppress("unused")
object EventBus {
    private val map: MutableMap<Class<*>, MutableList<Consumer<*>>> = ConcurrentHashMap()

    /**
     * Subscribe an object to the event bus, turning methods defined in it into listeners using @SubscribeEvent.
     *
     * @param obj The object to subscribe.
     * @see SubscribeEvent
     */
    @JvmStatic
    fun subscribe(obj: Any) = obj.javaClass.declaredMethods
        .filter { it.isAnnotationPresent(SubscribeEvent::class.java) && it.parameterCount == 1 }
        .forEach { getListeners(it.parameterTypes.first()) += ReflectEventConsumer(obj, it) }

    /**
     * Subscribe a listener to the event bus.
     *
     * @param event   The class of the event to subscribe to.
     * @param handler The Consumer to handle that event.
     */
    @JvmStatic
    fun <T : Event?> subscribe(event: Class<T>, handler: Consumer<T>) {
        getListeners(event) += handler
    }

    /**
     * Post an event for all the listeners listening for it.
     *
     * @param event The event to call.
     */
    @JvmStatic
    fun <T : Event> postEvent(event: T) {
        var curr: Class<*> = event.javaClass

        while (curr != Any::class.java) {
            getListeners(curr).filterIsInstance<Consumer<T>>().forEach(Consumer { it.accept(event) })
            curr = curr.superclass
        }
    }

    /**
     * Unsubscribe a listener from the event bus.
     *
     * @param consumer The Consumer to unsubscribe.
     */
    @JvmStatic
    fun unsubscribe(consumer: Consumer<Event>) = map.values.forEach { it.removeIf { c -> c === consumer } }

    /**
     * Unsubscribe an object from the event bus, which unsubscribes all of its listeners.
     *
     * @param obj The object to unsubscribe.
     */
    @JvmStatic
    fun unsubscribe(obj: Any) =
        map.values.forEach { it.removeIf { c -> c is ReflectEventConsumer && c.obj === obj } }

    /**
     * Returns a list of listeners alongside its event class.
     *
     * @param event The corresponding event class to grab the listeners from.
     * @return a list of listeners corresponding to the event class.
     */
    private fun getListeners(event: Class<*>) = map.computeIfAbsent(event) { CopyOnWriteArrayList() }

    private class ReflectEventConsumer(val obj: Any, val method: Method) : Consumer<Event?> {
        override fun accept(event: Event?) {
            method.invoke(obj, event)
        }
    }
}
