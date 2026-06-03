package net.weavemc.internals

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
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

public val Path.sha256sum: String
    get() = MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(this))
        .joinToString("") { "%02x".format(it) }

public val File.sha256sum: String
    get() = toPath().sha256sum

internal fun String.splitAround(c: Char): Pair<String,String> =
    substringBefore(c) to substringAfter(c, "")