package net.weavemc.loader.analytics

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

    val average = "%.1fs".format(launchTimes.average() / 1000)

    launchData.averageLaunchTime = average
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
    var launchTimes: MutableList<Long> = mutableListOf(),
    var timePlayed: Long = 0L,
    var averageLaunchTime: String = "0s"
)
