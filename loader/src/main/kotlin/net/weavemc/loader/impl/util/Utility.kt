package net.weavemc.loader.impl.util

import kotlinx.serialization.json.Json
import me.xtrm.klog.dsl.klog
import net.weavemc.internals.GameInfo
import net.weavemc.internals.ModConfig
import net.weavemc.internals.crc32sum
import net.weavemc.internals.getOrCreateWeaveDir
import net.weavemc.loader.impl.WeaveLoader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.*
import java.util.jar.JarFile
import javax.swing.JOptionPane
import kotlin.io.path.*
import kotlin.system.measureTimeMillis

public fun ByteArray.asClassReader(): ClassReader = ClassReader(this)
public fun ClassReader.asClassNode(): ClassNode = ClassNode().also { this.accept(it, 0) }

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
            if (javaVersion <= 19) {
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

public val javaVersion: Int by lazy {
    val version = System.getProperty("java.version", "1.6.0")
    val part = if (version.startsWith("1.")) version.split(".")[1] else version.substringBefore(".")
    part.toInt()
}

public val weaveLoaderData: Properties by lazy {
    val url = WeaveLoader::class.java.classLoader.getResource("weave-loader-data.properties")
    val stream = url?.openStream()

    Properties().apply { stream?.use { load(it) } }
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

public val cacheManager: CacheManager by lazy {
    CacheManager(
        getOrCreateWeaveDir(
            ".cache",
            "jars",
            "${MappingsHandler.environmentRuntimeNamespace}_${GameInfo.version.versionName}"
        )
    )
}

public fun File.createRemappedCache(
    fromNamespace: String,
    classpath: List<File> = listOf(MappingsHandler.minecraftRuntimeJar),
    deleteOnExit: Boolean = true,
): File {
    fun Path.createLock() {
        val lock = cacheManager.createLock(this, tryDeleteOnExit = deleteOnExit)
        runCatching {
            lock.writeText("original: $absolutePath")
        }.onFailure { e ->
            klog.warn("Failed to write lock metadata to $lock for $absolutePath", e)
        }
    }

    val earlyCache = cacheManager.find(crc32sum)?.apply(Path::createLock)
    if (earlyCache != null) {
        klog.debug("Found cached remapped JAR for '$name' ($absolutePath) at '${earlyCache.absolutePathString()}'")
        return earlyCache.toFile()
    }

    klog.info("Remapping JAR '$name' from namespace '$fromNamespace'...")

    val copyTemp = try {
        Files.createTempFile("weave-loader-remap", ".jar").apply {
            toFile().deleteOnExit()
        }
    } catch (e: Exception) {
        klog.error("Failed to create temporary file for remapping '$name'", e)
        throw e
    }

    klog.debug("Created temporary file for remapping at: ${copyTemp.absolutePathString()}")

    val time = measureTimeMillis {
        MappingsHandler.remapModJar(
            mappings = MappingsHandler.mergedMappings.mappings,
            input = this,
            output = copyTemp.toFile(),
            classpath = classpath,
            from = fromNamespace
        )
    }

    // ensure the file has been remapped successfully before copying to cache
    val cache = cacheManager.create(toPath()).apply(Path::createLock)

    try {
        copyTemp.moveTo(cache, overwrite = true)
        copyTemp.deleteIfExists()
    } catch (e: Exception) {
        klog.error("Failed to move remapped temp file '${copyTemp.absolutePathString()}' to cache location '${cache.absolutePathString()}'", e)
        throw e
    }

    klog.debug("Successfully remapped '$name' ($absolutePath) to '${cache.absolutePathString()}' in ${time}ms")

    return cache.toFile()
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
        classExists("net.minecraftforge.fml.common.Loader")
                || classExists("cpw.mods.fml.common.Loader") -> "forge"
        classExists("net.fabricmc.loader.api.FabricLoader") -> "fabric"
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