package net.weavemc.weave.api.mixin

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Local(
    val ordinal: Int = 0
)
