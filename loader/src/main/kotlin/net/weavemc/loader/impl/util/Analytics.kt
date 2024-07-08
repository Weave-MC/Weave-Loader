package net.weavemc.loader.impl.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private fun <T> Iterable<T>.atMost(capacity: Int) = when (this) {
    is Collection<T> -> if (size <= capacity) this else take(capacity)
    else -> take(capacity)
}

// I don't know why I decided to overengineer this
// Could also make it a pointer but then the order does not mean that much anymore
private class CircularBuffer<T>(
    private val capacity: Int,
    private val backing: MutableList<T> = mutableListOf()
) : MutableList<T> by backing {
    init {
        require(backing.size <= capacity) { "Backing list has too large of an initial size!" }
    }

    override fun add(element: T): Boolean {
        if (size >= capacity) removeFirst()
        backing.add(element)
        return true
    }

    override fun add(index: Int, element: T) {
        if (size >= capacity) removeFirst()
        backing.add(index, element)
    }

    private fun removeN(n: Int) {
        if (n <= 0) return
        repeat(minOf(capacity, n)) { removeFirst() }
    }

    // Not recommended, does not make a lot of sense for a circular buffer
    // For that reason, the implementation is basic/inefficient
    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false
        elements.forEach { add(index, it) }
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false

        removeN(elements.size - capacity)
        backing.addAll(elements.atMost(capacity))

        return true
    }

    fun toList() = backing.toList()
}

private fun <T> Iterable<T>.toCircularBuffer(capacity: Int) = CircularBuffer(capacity, atMost(capacity).toMutableList())

internal var launchStart = 0L

internal fun updateLaunchTimes() {
    val now = System.currentTimeMillis()
    val time = now - launchStart

    val analytics = getOrCreateAnalyticsFile()
    val json = analytics.readText()

    val launchData = if (json.isNotEmpty()) Json.decodeFromString(json) else Analytics()
    val launchTimes = launchData.launchTimes.toCircularBuffer(capacity = 10)
    launchTimes += time

    analytics.writeText(
        Json.encodeToString(
            launchData.copy(
                launchTimes = launchTimes,
                averageLaunchTime = (launchTimes.average() / 1000).toFloat()
            )
        )
    )
}

private fun getOrCreateAnalyticsFile(): Path {
    val file = Paths.get(System.getProperty("user.home"), ".weave", "analytics.json")
    if (!file.exists()) file.createFile()

    return file
}

@Serializable
private data class Analytics(
    @SerialName("launch_times") val launchTimes: List<Long> = emptyList(),
    @SerialName("time_played") val timePlayed: Long = 0L,
    @SerialName("average_launch_time") val averageLaunchTime: Float = 0f
)
