package club.maxstats.weave.loader.api.event

import java.lang.reflect.Method
import java.util.function.Consumer

public object EventBus {

    private val map = hashMapOf<Class<*>, MutableList<Consumer<*>>>()
    private val Class<*>.listeners get() = map.getOrPut(this) { mutableListOf() }

    public fun subscribe(obj: Any) {
        obj.javaClass.declaredMethods
            .filter { it.isAnnotationPresent(SubscribeEvent::class.java) && it.parameterCount == 1 }
            .forEach { it.parameterTypes.first().listeners += ReflectEventHandler(obj, it) }
    }

    public fun <T : Event> subscribe(type: Class<T>, handler: Consumer<T>) {
        type.listeners += handler
    }

    public inline fun <reified T : Event> subscribe(noinline handler: (T) -> Unit) {
        subscribe(T::class.java, handler)
    }

    public fun unsubscribe(obj: Any) {
        for (list in map.values) {
            list.removeAll { it is ReflectEventHandler && it.obj == obj }
        }
    }

    public fun <T : Event> callEvent(event: T) {
        event.javaClass.listeners.forEach { listener ->
            try {
                @Suppress("UNCHECKED_CAST")
                (listener as Consumer<T>).accept(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

private class ReflectEventHandler(val obj: Any, val method: Method) : Consumer<Event> {

    override fun accept(t: Event) {
        method(obj, t)
    }

}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
public annotation class SubscribeEvent
