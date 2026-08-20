package net.weavemc.internals

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.CRC32
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Grabs the directory for the specified directory, creating it if it doesn't exist.
 * If the file exists as a file and not a directory, it will be deleted.
 *
 * @param directories The directories to grab.
 * @return The specified directory: `"~/.weave/<directory>"`
 */
public fun getOrCreateWeaveDir(vararg directories: String): Path {
    val dir = Paths.get(System.getProperty("user.home"), ".weave", *directories)
    if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
    if (!dir.exists()) dir.createDirectories()
    return dir
}

public val Path.crc32sum: String
    get() {
        val crc = CRC32()

        Files.newInputStream(this).buffered().use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead = stream.read(buffer)
            while (bytesRead != -1) {
                crc.update(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
        }

        return String.format("%08x", crc.value)
    }

public val File.crc32sum: String
    get() = toPath().crc32sum

internal fun String.splitAround(c: Char): Pair<String,String> =
    substringBefore(c) to substringAfter(c, "")