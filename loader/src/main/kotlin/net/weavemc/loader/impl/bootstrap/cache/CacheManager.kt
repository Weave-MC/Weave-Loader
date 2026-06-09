package net.weavemc.loader.impl.bootstrap.cache

import kotlinx.coroutines.*
import net.weavemc.internals.crc32sum
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

public class CacheManager(public val cacheDirectory: Path) {
    public val id: String = "${hashCode()}${abs(random.nextInt())}${System.currentTimeMillis()}"

    public val timeFile: Path = cacheDirectory / "time.$id"

    init {
        cacheDirectory.createDirectories()

        timeFile.toFile().run {
            createNewFile()
            deleteOnExit()
        }
        CoroutineScope(Dispatchers.Default).scheduleTimeUpdateTask()
    }

    public val activeCacheFiles: MutableSet<Path> = mutableSetOf()

    public fun find(crc32sum: String): Path? =
        (cacheDirectory / crc32sum.cacheFile).takeIf { it.exists() }

    public fun create(original: Path): Path {
        if (!original.exists()) {
            throw IllegalArgumentException("Cannot create from a non-existent file: ${original.absolutePathString()}")
        }

        return cacheDirectory / original.crc32sum.cacheFile
    }

    /**
     * @return The lock's path.
     */
    public fun createLock(parent: Path, tryDeleteOnExit: Boolean = true): Path {
        val lockFile = if (tryDeleteOnExit) parent.lockFile else parent.lockFileUniversal

        if (!lockFile.exists()) {
            lockFile.createFile()
        }

        if (tryDeleteOnExit) {
            Runtime.getRuntime().addShutdownHook(Thread {
                lockFile.deleteIfExists()
            })
        }

        return lockFile
    }

    public fun updateTime(): Unit = timeFile.writeText(System.currentTimeMillis().toString())

    public fun readTime(timeFile: Path): Long? = timeFile.readText().trim().toLongOrNull()

    public fun cleanup() {
        cleanupTimeAndLockFiles()
        cleanupCacheFiles()
    }

    private fun cleanupTimeAndLockFiles() {
        val currentTime = System.currentTimeMillis()
        val duration = (duration - 10.seconds).inWholeMilliseconds

        val upToDateTimes = cacheDirectory.listDirectoryEntries().mapNotNull { file ->
            val time = file.readRegex(timeFileRegex) ?: return@mapNotNull null

            fun Path.deleteAndNull(): Any? {
                deleteExisting()
                return null
            }

            val targetTime = readTime(file) ?: return@mapNotNull file.deleteAndNull()

            if (currentTime - targetTime > duration) return@mapNotNull file.deleteAndNull()

            time
        } + PERMANENT_TIME_ID

        val outdatedLocks = cacheDirectory
            .listDirectoryEntries()
            .filter {
                val lockId = it.readRegex(crc32LockRegex, 2) ?: return@filter false
                lockId !in upToDateTimes
            }

        outdatedLocks.forEach(Path::deleteExisting)
    }

    private fun cleanupCacheFiles() {
        val files = cacheDirectory.listDirectoryEntries()

        val activeFiles = activeCacheFiles.mapNotNull { it.readRegex(crc32CacheRegex) }
        val lockedFileSums = files.mapNotNull { it.readRegex(crc32LockRegex) }.distinct()

        val filesWithoutLocks = files
            .filter { it.isRegularFile() }
            .filter {
                val checksum = it.readRegex(crc32CacheRegex) ?: return@filter false
                checksum !in activeFiles && checksum !in lockedFileSums
            }

        filesWithoutLocks.forEach(Path::deleteExisting)
    }

    public val String.cacheFile: Path get() = Path("$this.cache")

    public val Path.lockFileUniversal: Path get() = resolveSibling("$fileName.lock.$PERMANENT_TIME_ID")

    public val Path.lockFile: Path get() = resolveSibling("$fileName.lock.$id")

    private fun CoroutineScope.scheduleTimeUpdateTask(): Job = launch(Dispatchers.IO) {
        while (isActive) {
            updateTime()

            delay(duration)
        }
    }

    private fun Path.readRegex(regex: Regex, groupIndex: Int = 1) = regex.find(name)?.groupValues?.get(groupIndex)

    public companion object {
        private const val PERMANENT_TIME_ID = "0"

        private val crc32CacheRegex = Regex("([a-fA-F0-9]{8})\\.cache")
        private val crc32LockRegex = Regex("([a-fA-F0-9]{8})\\.cache\\.lock\\.([0-9]+)")
        private val timeFileRegex = Regex("time\\.([0-9]+)")

        private val duration = 8.hours

        private val random = Random()
    }
}