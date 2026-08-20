package net.weavemc.loader.impl.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.xtrm.klog.dsl.klog
import net.weavemc.internals.crc32sum
import java.nio.file.Path
import java.util.Random
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

public class CacheManager(public val cacheDirectory: Path) {
    public val id: String = "${hashCode()}${abs(random.nextInt())}${System.currentTimeMillis()}"

    public val timeFile: Path = cacheDirectory / "time.$id"

    init {
        logger.debug("Initialising CacheManager [ID: $id] at directory: ${cacheDirectory.absolutePathString()}")

        runCatching {
            cacheDirectory.createDirectories()
            timeFile.toFile().run {
                createNewFile()
                deleteOnExit()
            }
        }.onFailure { e ->
            logger.error("Failed to initialise cache directory or time file for process ID: $id", e)
        }

        CoroutineScope(Dispatchers.Default).scheduleTimeUpdateTask()
    }

    public val activeCacheFiles: MutableSet<Path> = mutableSetOf()

    public fun find(crc32sum: String): Path? {
        val target = cacheDirectory / crc32sum.cacheFile
        return if (target.exists()) {
            logger.trace("Cache hit for CRC32 sum '$crc32sum' at: ${target.absolutePathString()}")
            target
        } else {
            logger.trace("Cache miss for CRC32 sum '$crc32sum'")
            null
        }
    }

    public fun create(original: Path): Path {
        require(original.exists()) {
            "Cannot create cache entry from a non-existent file: ${original.absolutePathString()}"
        }

        val cachePath = cacheDirectory / original.crc32sum.cacheFile
        logger.debug("Mapped original file '${original.name}' to cache path: ${cachePath.absolutePathString()}")
        return cachePath
    }

    /**
     * @return The lock's path.
     */
    public fun createLock(parent: Path, tryDeleteOnExit: Boolean = true): Path {
        val lockFile = if (tryDeleteOnExit) parent.lockFile else parent.lockFileUniversal
        if (!lockFile.exists()) {
            runCatching {
                lockFile.createFile()
                logger.trace("Created lock file: ${lockFile.absolutePathString()}")
            }.onFailure { e ->
                logger.warn("Failed to create lock file at: ${lockFile.absolutePathString()}", e)
            }
        }

        if (tryDeleteOnExit) {
            Runtime.getRuntime().addShutdownHook(Thread {
                runCatching {
                    if (lockFile.deleteIfExists()) {
                        logger.trace("Shutdown hook removed lock file: ${lockFile.name}")
                    }
                }.onFailure { e ->
                    logger.warn("Failed to delete lock file on process exit: ${lockFile.absolutePathString()}", e)
                }
            })
        }

        return lockFile
    }

    public fun updateTime() {
        val now = System.currentTimeMillis()

        runCatching {
            timeFile.writeText(now.toString())
            logger.trace("Updated process time file ($id) with timestamp: $now")
        }.onFailure { e ->
            logger.warn("Failed to update process time file: ${timeFile.absolutePathString()}", e)
        }
    }

    public fun readTime(timeFile: Path): Long? = runCatching {
        timeFile.readText().trim().toLongOrNull()
    }.getOrNull()

    public fun cleanup() {
        logger.info("Starting cache directory cleanup sweep...")

        val duration = measureTime {
            cleanupTimeAndLockFiles()
            cleanupCacheFiles()
        }

        logger.info("Cache cleanup completed in $duration")
    }

    private fun cleanupTimeAndLockFiles() {
        val currentTime = System.currentTimeMillis()
        val duration = (duration - 10.seconds).inWholeMilliseconds
        var deletedTimeFiles = 0

        val upToDateTimes = cacheDirectory.listDirectoryEntries().mapNotNull { file ->
            val time = file.readRegex(timeFileRegex) ?: return@mapNotNull null

            fun Path.deleteAndNull(): String? {
                runCatching {
                    deleteExisting()
                    deletedTimeFiles++
                    logger.debug("Deleted stale process time file: ${this.name}")
                }.onFailure { e ->
                    logger.warn("Failed to delete stale time file: ${this.absolutePathString()}", e)
                }
                return null
            }

            val targetTime = readTime(file) ?: return@mapNotNull file.deleteAndNull()

            if (currentTime - targetTime > duration) {
                logger.debug("Process time file '${file.name}' expired (Age: ${(currentTime - targetTime) / 1000}s)")
                return@mapNotNull file.deleteAndNull()
            }

            time
        } + PERMANENT_TIME_ID

        var deletedLockFiles = 0
        val outdatedLocks = cacheDirectory
            .listDirectoryEntries()
            .filter {
                val lockId = it.readRegex(crc32LockRegex, 2) ?: return@filter false
                lockId !in upToDateTimes
            }

        outdatedLocks.forEach { lockFile ->
            runCatching {
                lockFile.deleteExisting()
                deletedLockFiles++
                logger.debug("Deleted orphaned lock file: ${lockFile.name}")
            }.onFailure { e ->
                logger.warn("Failed to delete orphaned lock file: ${lockFile.absolutePathString()}", e)
            }
        }

        logger.debug("Time & Lock Cleanup Summary: Deleted $deletedTimeFiles stale time file(s) and $deletedLockFiles orphaned lock file(s). Active process IDs: ${upToDateTimes.size}")
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

        var deletedCacheFiles = 0
        for (cacheFile in filesWithoutLocks) {
            runCatching {
                cacheFile.deleteExisting()
                deletedCacheFiles++
                logger.info("Deleted unreferenced cache file: ${cacheFile.name}")
            }.onFailure { e ->
                logger.warn("Failed to delete unreferenced cache file: ${cacheFile.absolutePathString()}", e)
            }
        }

        logger.debug("Cache Files Cleanup Summary: Removed $deletedCacheFiles unused cache file(s).")
        logger.debug("Current cache state: ${activeFiles.size} active file(s), ${lockedFileSums.size} locked sum(s).")
    }

    public val String.cacheFile: Path get() = Path("$this.cache")

    public val Path.lockFileUniversal: Path get() = resolveSibling("$fileName.lock.$PERMANENT_TIME_ID")

    public val Path.lockFile: Path get() = resolveSibling("$fileName.lock.$id")

    private fun CoroutineScope.scheduleTimeUpdateTask(): Job = launch(Dispatchers.IO) {
        logger.debug("Started background process time update loop for ID: $id (Interval: $duration)")
        while (isActive) {
            updateTime()
            delay(duration)
        }
    }

    private fun Path.readRegex(regex: Regex, groupIndex: Int = 1) = regex.find(name)?.groupValues?.get(groupIndex)

    public companion object {
        private val logger by klog

        private const val PERMANENT_TIME_ID = "0"

        private val crc32CacheRegex = Regex("([a-fA-F0-9]{8})\\.cache")
        private val crc32LockRegex = Regex("([a-fA-F0-9]{8})\\.cache\\.lock\\.([0-9]+)")
        private val timeFileRegex = Regex("time\\.([0-9]+)")

        private val duration by systemProperty("weave.cache.duration", 8.hours)

        private val random = Random()
    }
}