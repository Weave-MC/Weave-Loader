package net.weavemc.loader.analytics

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

internal var launchStart = 0L

internal fun updateLaunchTimes() {
    val now = System.currentTimeMillis()
    val time = now - launchStart

    val analytics = getOrCreateAnalyticsFile()
    val json = analytics.readText()

    val launchData = if (json.isNotEmpty()) Json.decodeFromString(json) else Analytics()
    val launchTimes = launchData.launchTimes.toMutableList()

    if (launchTimes.size >= 10) launchTimes.removeFirst()
    launchTimes += time

    analytics.writeText(
        Json.encodeToString(
            launchData.copy(
                launchTimes = launchTimes,
                averageLaunchTime = launchTimes.average().toFloat() / 1000f
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
