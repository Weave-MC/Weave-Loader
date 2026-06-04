package net.weavemc.loader.impl.bootstrap.cache

import net.weavemc.internals.crc32sum
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*

public class CacheManager(public val cacheDirectory: Path) {
    init {
        cacheDirectory.createDirectories()
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
        val lockFile = parent.lockFile

        if (!lockFile.exists()) {
            lockFile.createFile()
        }

        if (tryDeleteOnExit) {
            val channel = FileChannel.open(
                lockFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ
            )

            val sharedLock = channel.lock(0L, Long.MAX_VALUE, true)

            Runtime.getRuntime().addShutdownHook(Thread {
                if (sharedLock.isValid){
                    sharedLock.release()
                }

                val exclusiveLock = channel.tryLock(0L, Long.MAX_VALUE, false)

                if (exclusiveLock != null) {
                    exclusiveLock.release()
                    channel.close()
                    Files.deleteIfExists(lockFile)
                } else {
                    channel.close()
                }
            })
        }

        return lockFile
    }

    public fun deleteLock(parent: Path): Boolean = parent.lockFile.deleteIfExists()

    public fun cleanup() {
        val activeFilesFilter = activeCacheFiles.map { it.name }

        val filesWithoutLocks = cacheDirectory
            .listDirectoryEntries()
            .filter { it.name.matches(crc32CacheRegex) }
            .filter { it.isRegularFile() }
            .filter { it.lockFile.notExists() }
            .filter { it.name !in activeFilesFilter }

        filesWithoutLocks.forEach(Path::deleteExisting)
    }

    public val String.cacheFile: Path get() = Path("$this.cache")

    public val Path.lockFile: Path get() = resolveSibling("$fileName.lock")

    private companion object {
        val crc32CacheRegex = Regex("^[a-fA-F0-9]{8}\\.cache$")
    }
}