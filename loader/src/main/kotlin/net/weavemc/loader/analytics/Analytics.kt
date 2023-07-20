package net.weavemc.loader.analytics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

internal var launchStart = 0L

internal fun updateLaunchTimes() {
    val now = System.currentTimeMillis()
    val time = now - launchStart

    val analytics = getOrCreateAnalyticsFile()
    val json = analytics.readText()

    val launchData = if (json.isNotEmpty())
        Json.decodeFromString<Analytics>(json)
    else
        Analytics()

    val launchTimes = launchData.launchTimes
    if (launchTimes.size >= 10)
        launchTimes.removeAt(0)
    launchTimes.add(time)

    launchData.averageLaunchTime = (launchTimes.average().toFloat() / 1000)
    launchData.launchTimes = launchTimes

    val updatedJson = Json.encodeToString(launchData)

    analytics.writeText(updatedJson)
}

private fun getOrCreateAnalyticsFile(): File {
    val file = File("${System.getProperty("user.home")}/.weave/analytics.json")
    if (!file.exists())
        file.createNewFile()

    return file
}

@Serializable
private data class Analytics(
    @SerialName("launch_times") var launchTimes: MutableList<Long> = mutableListOf(),
    @SerialName("time_played") var timePlayed: Long = 0L,
    @SerialName("average_launch_time") var averageLaunchTime: Float = 0f
)
