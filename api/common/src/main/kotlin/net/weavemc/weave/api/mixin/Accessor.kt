package net.weavemc.weave.api.mixin

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Accessor(
    val target: String = ""
)
