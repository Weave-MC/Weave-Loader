package net.weavemc.api.mixin

annotation class ModifyArg(
    val id: String = "",
    val method: String = "",
    val invokedMethod: String = "",
    val index: Int = -1,
    val shift: Int = 0,
)