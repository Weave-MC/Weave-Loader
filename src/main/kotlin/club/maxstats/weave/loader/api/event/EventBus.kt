package club.maxstats.weave.loader.api.event

import club.maxstats.weave.loader.api.event.annotation.SubscribeEvent
import java.lang.reflect.Method

object EventBus {
    private data class Listener(val obj: Any, val method: Method)

    private val map = hashMapOf<Class<*>, MutableList<Listener>>()

    fun subscribe(obj: Any) =
        obj.javaClass.declaredMethods
            .filter {
                it.isAnnotationPresent(SubscribeEvent::class.java)
                        && it.parameterCount == 1
            }
            .forEach {
                map.getOrPut(it.parameterTypes[0]) { mutableListOf() } += Listener(obj, it)
            }

    fun unsubscribe(obj: Any) {
        for (list in map.values) {
            list.removeAll { it.obj == obj }
        }
    }

    fun callEvent(event: Event) {
        map[event.javaClass]?.forEach { l ->
            try {
                l.method(l.obj, event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}