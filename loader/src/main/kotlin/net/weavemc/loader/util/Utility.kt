package net.weavemc.loader.util

import kotlinx.serialization.json.Json
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory

/**
 * Grabs the directory for the specified directory, creating it if it doesn't exist.
 * If the file exists as a file and not a directory, it will be deleted.
 *
 * @param directory The directory to grab.
 * @return The specified directory: `"~/.weave/<directory>"`
 */
internal fun getOrCreateDirectory(directory: String): Path {
    val dir = Paths.get(System.getProperty("user.home"), ".weave", directory)
    if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
    if (!dir.exists()) dir.createDirectories()
    return dir
}

internal fun ByteArray.asClassReader(): ClassReader = ClassReader(this)
internal fun ClassReader.asClassNode(): ClassNode = ClassNode().also { this.accept(it, 0) }

internal fun File.toSha256(): String {
    val bytes = Files.readAllBytes(toPath())
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val digest = messageDigest.digest(bytes)
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}

internal val JSON = Json { ignoreUnknownKeys = true }

internal inline fun <reified T> Set<T>.pushToFirst(element: T): MutableList<T> =
    if (element in this)
        mutableListOf(element).also { it.addAll(this) }
    else this.toMutableList()

// Copied from Weave-Gradle
object DownloadUtil {
    /**
     * Returns the SHA1 checksum of the file as a [String]
     *
     * @param file The file to check.
     * @return the SHA1 checksum of the file.
     */
    private fun checksum(file: Path) = try {
        if (!file.exists()) null
        else {
            val digest = MessageDigest.getInstance("SHA-1")
            file.inputStream().use { input ->
                val buffer = ByteArray(0x2000)
                var read: Int

                while (input.read(buffer).also { read = it } >= 0) {
                    digest.update(buffer, 0, read)
                }
            }

            digest.digest().joinToString { "%02x".format(it) }
        }
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    } catch (ignored: NoSuchAlgorithmException) {
        null
    }

    /**
     * Downloads a file from any URL
     *
     * @param url The URL to download from.
     * @param path The path to download to.
     */
    private fun download(url: URL, path: Path) {
        runCatching {
            url.openStream().use { input ->
                Files.createDirectories(path.parent)
                Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
            }
        }.onFailure { it.printStackTrace() }
    }

    fun download(url: String, path: String) = download(URL(url), Paths.get(path))

    /**
     * Fetches data from any URL
     *
     * @param url The URL to download from
     */
    private fun fetch(url: URL) = runCatching { url.openStream().readBytes().decodeToString() }
        .onFailure { it.printStackTrace() }.getOrNull()

    fun fetch(url: String) = fetch(URL(url))

    /**
     * Downloads and checksums a file.
     *
     * @param url The URL to download from.
     * @param checksum The checksum to compare to.
     * @param path The path to download to.
     */
    fun checksumAndDownload(url: URL, checksum: String, path: Path) {
        if (checksum(path) != checksum) download(url, path)
    }
}
