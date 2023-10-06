package net.weavemc.weave.api.mixin

annotation class ModifyArg(
    val id: String = "",
    val method: String = "",
    val invokedMethod: String = "",
    val index: Int = -1,
)
