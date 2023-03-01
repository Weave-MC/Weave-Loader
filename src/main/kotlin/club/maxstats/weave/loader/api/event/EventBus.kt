package club.maxstats.weave.loader.api.event

import java.lang.reflect.Method

object EventBus {
    private val map = hashMapOf<Class<*>, MutableList<EventHandler<*>>>()
    private val Class<*>.listeners get() = map.getOrPut(this) { mutableListOf() }

    fun subscribe(obj: Any) =
        obj.javaClass.declaredMethods
            .filter { it.isAnnotationPresent(SubscribeEvent::class.java) && it.parameterCount == 1 }
            .forEach { it.parameterTypes.first().listeners += ClassEventHandler(obj, it) }

    fun <T : Event> subscribe(type: Class<T>, handler: (T) -> Unit) {
        type.listeners += FunctionEventHandler(handler)
    }

    inline fun <reified T : Event> subscribe(noinline handler: (T) -> Unit) = subscribe(T::class.java, handler)

    fun unsubscribe(obj: Any) =
        map.values.forEach { list -> list.removeAll { it is ClassEventHandler && it.obj == obj } }

    fun <T : Event> callEvent(event: T) = event.javaClass.listeners.forEach { listener ->
        try {
            @Suppress("UNCHECKED_CAST")
            (listener as EventHandler<T>)(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

sealed interface EventHandler<T : Event> {
    fun handle(event: T)
}

operator fun <T : Event> EventHandler<T>.invoke(event: T) = handle(event)

class ClassEventHandler(internal val obj: Any, internal val method: Method) : EventHandler<Event> {
    override fun handle(event: Event) {
        method(obj, event)
    }
}

class FunctionEventHandler<T : Event>(private val callback: (T) -> Unit) : EventHandler<T> {
    override fun handle(event: T) = callback(event)
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SubscribeEvent

abstract class Event