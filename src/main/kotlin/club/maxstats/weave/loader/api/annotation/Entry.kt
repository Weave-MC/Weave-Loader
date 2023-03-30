package club.maxstats.weave.loader.api.annotation

import club.maxstats.weave.loader.api.WeavePhase

@Retention(AnnotationRetention.RUNTIME)
public annotation class Entry(
    val phase: WeavePhase = WeavePhase.INIT,
    val priority: Int = 0
)
