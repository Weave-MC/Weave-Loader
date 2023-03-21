package club.maxstats.weave.loader.api

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Priority(val priority: Priorities)

enum class Priorities(private val priority: Int) {
    LOWEST(-2),
    LOW(-1),
    NORMAL(0),
    HIGH(1),
    HIGHEST(2);

    fun getPriority(): Int = priority
}
