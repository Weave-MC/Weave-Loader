package net.weavemc.loader.impl.util

import kotlinx.serialization.json.Json
import me.xtrm.klog.dsl.klog
import net.weavemc.internals.GameInfo
import net.weavemc.internals.ModConfig
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.jar.JarFile
import javax.swing.JOptionPane
import kotlin.io.path.*

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

internal val JSON = Json { ignoreUnknownKeys = true }

internal fun MethodNode.hasMixinAnnotation(name: String): Boolean {
    val annotation = "spongepowered/asm/mixin/transformer/meta/$name;"
    return visibleAnnotations?.any { it.desc.endsWith(annotation) } == true
}

internal inline fun <reified T> instantiate(className: String): T =
    Class.forName(className)
        .getConstructor()
        .newInstance() as? T
        ?: error("$className does not implement ${T::class.java.simpleName}!")

internal inline fun <reified T> instantiate(className: String, loader: ClassLoader?): T =
    Class.forName(className, false, loader)
        .getConstructor()
        .newInstance() as? T
        ?: error("$className does not implement ${T::class.java.simpleName}!")

internal fun fatalError(message: String): Nothing {
    klog.fatal("An error occurred: $message")
    JOptionPane.showMessageDialog(
        /* parentComponent = */ null,
        /* message = */ "An error occurred: $message",
        /* title = */ "Weave Loader error",
        /* messageType = */ JOptionPane.ERROR_MESSAGE
    )

    exit(-1)
}

/**
 * Exits the JVM with the given error code, escaping any SecurityManager
 * in place.
 *
 * @param errorCode the error code to exit with
 */
internal fun exit(errorCode: Int): Nothing {
    runCatching {
        val clazz = Class.forName("java.lang.Shutdown")
        clazz.getDeclaredMethod("exit", Int::class.javaPrimitiveType).apply {
            isAccessible = true
        }(null, errorCode)
    }.onFailure { e0 ->
        runCatching {
            exitRuntime(errorCode)
        }.onFailure { e1 ->
            if (getJavaVersion() <= 19) {
                @Suppress("DEPRECATION")
                AccessController.doPrivileged(PrivilegedAction<Void> {
                    exitRuntime(errorCode)
                    null
                })
            } else {
                e1.addSuppressed(e0)
                throw RuntimeException("Exiting the JVM, no errors to report here.", e1)
            }
        }
    }

    throw IllegalStateException("This should never be reached")
}

private fun exitRuntime(errorCode: Int) {
    val clazz = Class.forName("java.lang.Runtime")
    val runtime = clazz.getDeclaredMethod("getRuntime").also { it.isAccessible = true }(null)
    clazz.getDeclaredMethod("exit", Int::class.javaPrimitiveType).also { it.isAccessible = true }(runtime, errorCode)
}

internal fun getJavaVersion(): Int {
    val version = System.getProperty("java.version", "1.6.0")
    val part = if (version.startsWith("1.")) version.split(".")[1] else version.substringBefore(".")
    return part.toInt()
}

internal fun JarFile.configOrFatal() = runCatching { fetchModConfig(JSON) }.onFailure {
    klog.error("Possibly non-weave mod failed to load:")
    it.printStackTrace()

    fatalError("Mod file ${this.name} is possibly not a Weave mod!")
}.getOrThrow()

internal fun JarFile.fetchModConfig(json: Json): ModConfig {
    val configEntry = getEntry("weave.mod.json") ?: error("${this.name} does not contain a weave.mod.json!")
    return json.decodeFromString<ModConfig>(getInputStream(configEntry).readBytes().decodeToString())
}

internal fun File.createRemappedTemp(name: String, config: ModConfig): File {
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

internal fun setGameInfo() {
    val cwd = Path(System.getProperty("user.dir"))
    val version = System.getProperty("weave.environment.version")
        ?: cwd.takeIf { "instances" in it.pathString }?.run {
            val instance = cwd.parent
            runCatching {
                val instanceData = JSON.decodeFromString<MultiMCInstance>(
                    instance.resolve("mmc-pack.json").toFile().readText()
                )

                instanceData.components.find { it.uid == "net.minecraft" }?.version
            }.getOrNull()
        } ?: """--version\s+(\S+)""".toRegex()
            .find(System.getProperty("sun.java.command"))
            ?.groupValues?.get(1)
        ?: fatalError("Could not determine game version")

    fun classExists(name: String): Boolean =
        GameInfo::class.java.classLoader.getResourceAsStream("${name.replace('.', '/')}.class") != null

    val client = when {
        classExists("com.moonsworth.lunar.genesis.Genesis") -> "lunar client"
        classExists("net.minecraftforge.fml.common.Loader") -> "forge"
        GameInfo.commandLineArgs.contains("labymod") -> "labymod"
        else -> "vanilla"
    }

    System.getProperties()["weave.game.info"] = mapOf(
        "version" to version,
        "client" to client
    )
}

// TODO: give this a good place
internal val illegalToReload = setOf(
    "java.", "javax.", "org.xml.", "org.w3c.", "sun.", "jdk.",
    "com.sun.management.", "org.apache.", "org.slf4j."
)

internal data class WeaveMod(val modId: String, val config: ModConfig)