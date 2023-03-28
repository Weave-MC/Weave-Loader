package club.maxstats.weave.loader.bootstrap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.Mixins
import java.lang.instrument.Instrumentation
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.*

@Suppress("UNUSED_PARAMETER")
public fun premain(opt: String?, inst: Instrumentation) {
    if(findVersion() != "1.8.9") {
        println("[Weave] ${findVersion()} not supported, disabling...")
        return
    }

    inst.addTransformer(object : SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            if (className.startsWith("net/minecraft/")) {
                inst.removeTransformer(this)

                val configList = mutableListOf<WeaveModConfig>()
                getOrCreateModDirectory()
                    .listDirectoryEntries("*.jar")
                    .filter { it.isRegularFile() }
                    .map { it.toFile() }
                    .forEach { modFile ->
                        println("[Weave] Loading ${modFile.name}")
                        val jar = JarFile(modFile)
                        inst.appendToSystemClassLoaderSearch(jar)

                        val config = Json.decodeFromStream<WeaveModConfig>(
                            jar.getInputStream(
                                jar.getEntry("weave.mod.json") ?: error("${modFile.name} does not contain a weave.mod.json!")
                            )
                        )
                        configList += config
                    }

                loader.loadClass("club.maxstats.weave.loader.bootstrap.MixinLoader")
                    .getConstructor(List::class.java)
                    .newInstance(configList.flatMap { it.mixins })

                /*
                Load the rest of the loader using Genesis class loader.
                This allows us to access Minecraft's classes throughout the project.
                */
                loader.parent.loadClass("club.maxstats.weave.loader.WeaveLoader")
                    .getDeclaredMethod("preInit", Instrumentation::class.java, List::class.java)
                    .invoke(null, inst, configList.flatMap { it.entrypoints })
            }

            return null
        }
    })

    inst.addTransformer(GenesisTransformer)
}

private fun getOrCreateModDirectory(): Path {
    val dir = Paths.get(System.getProperty("user.home"), ".lunarclient", "mods")
    if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
    if (!dir.exists()) dir.createDirectory()
    return dir
}

private fun findVersion() =
    """--version ([^ ]+)""".toRegex()
        .find(System.getProperty("sun.java.command"))
        ?.groupValues?.get(1)

@Serializable
private data class WeaveModConfig(val mixins: List<String> = listOf(), val entrypoints: List<String>)

public class MixinLoader(mixins: List<String>) {
    init {
        println("[Weave] Initializing Mixins")

        MixinBootstrap.init()
        mixins.forEach { Mixins.addConfiguration(it) }

        println("[Weave] Initialized Mixins")
    }
}
