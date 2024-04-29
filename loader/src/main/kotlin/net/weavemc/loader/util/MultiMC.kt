package net.weavemc.loader.util

import kotlinx.serialization.Serializable

@Serializable
data class MultiMCInstance(
    val components: List<MultiMCComponent>
)
@Serializable
data class MultiMCComponent(
    val cachedName: String,
    val uid: String,
    val version: String
)