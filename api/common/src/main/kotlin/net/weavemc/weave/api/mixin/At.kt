package net.weavemc.weave.api.mixin

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class At(
    val id: String = "",
    val value: Location = Location.HEAD,
    val target: String = "",
    val shift: Shift = Shift.NONE,
    val by: Int = 0
) {
    public enum class Shift {
        NONE, BEFORE, AFTER
    }

    public enum class Location {
        HEAD, RETURN, INVOKE, PUTFIELD, GETFIELD
    }
}
