package net.weavemc.weave.api.mixin

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Overwrite(
    val id: String = "",
    val method: String = "",
)
