package net.weavemc.weave.api.mixin

annotation class ModifyArgs(
    val id: String = "",
    val method: String = "",
    val invokedMethod: String = "",
    val shift: Int = 0,
)
