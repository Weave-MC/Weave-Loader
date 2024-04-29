package net.weavemc.loader.util

import kotlinx.serialization.json.Json
import net.weavemc.internals.ModConfig
import net.weavemc.loader.fetchModConfig
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.jar.JarFile
import javax.swing.JOptionPane
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

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

internal inline fun <reified T> Set<T>.pushToFirst(element: T): List<T> =
    listOfNotNull(element.takeIf { it in this }) + (this - element)

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

fun MethodNode.hasMixinAnnotation(name: String): Boolean {
    val annotation = "spongepowered/asm/mixin/transformer/meta/$name;"
    return visibleAnnotations?.any { it.desc.endsWith(annotation) } == true
}

inline fun <reified T> instantiate(className: String): T =
    Class.forName(className)
        .getConstructor()
        .newInstance() as? T
        ?: error("$className does not implement ${T::class.java.simpleName}!")

internal fun fatalError(message: String): Nothing {
    JOptionPane.showMessageDialog(
        /* parentComponent = */ null,
        /* message = */ "An error occurred: $message",
        /* title = */ "Weave Loader error",
        /* messageType = */ JOptionPane.ERROR_MESSAGE
    )

    exitProcess(-1)
}

fun JarFile.configOrFatal() = runCatching { fetchModConfig(JSON) }.onFailure {
    println("Possibly non-weave mod failed to load:")
    it.printStackTrace()

    fatalError("Mod file ${this.name} is possibly not a Weave mod!")
}.getOrThrow()

fun JarFile.fetchModConfig(json: Json): ModConfig {
    val configEntry = getEntry("weave.mod.json") ?: error("${this.name} does not contain a weave.mod.json!")
    return json.decodeFromString<ModConfig>(getInputStream(configEntry).readBytes().decodeToString())
}

fun File.createRemappedTemp(name: String, config: ModConfig): File {
    val temp = File.createTempFile(name, "-weavemod.jar")
    MappingsHandler.remapModJar(
        mappings = MappingsHandler.mergedMappings.mappings,
        input = this,
        output = temp,
        classpath = listOf(FileManager.getVanillaMinecraftJar()),
        from = config.namespace
    )

    temp.deleteOnExit()
    return temp
}

// TODO: give this a good place
val illegalToReload = setOf(
    "java.", "javax.", "org.xml.", "org.w3c.", "sun.", "jdk.",
    "com.sun.management.", "org.apache.", "org.slf4j."
)

data class WeaveMod(val modId: String, val config: ModConfig)