package net.weavemc.weave.api.mixin

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Inject(
    val id: String = "",
    val method: String = "",
    val at: At = At()
)
