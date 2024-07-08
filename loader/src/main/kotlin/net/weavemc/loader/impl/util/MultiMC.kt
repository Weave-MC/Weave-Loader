package net.weavemc.loader.impl.util

import kotlinx.serialization.Serializable

@Serializable
internal data class MultiMCInstance(
    val components: List<MultiMCComponent>
)

@Serializable
internal data class MultiMCComponent(
    val cachedName: String,
    val uid: String,
    val version: String? = null
)