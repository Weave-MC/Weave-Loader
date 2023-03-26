package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import kotlinx.serialization.ExperimentalSerializationApi
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

public object WeaveLoader {
    private val hookManager = HookManager()

    /**
     * @see [club.maxstats.weave.loader.bootstrap.premain]
     */
    @JvmStatic
    @OptIn(ExperimentalSerializationApi::class)
    public fun preInit(inst: Instrumentation, classLoader: ClassLoader) {
        inst.addTransformer(hookManager.Transformer())

        MixinBootstrap.init()

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
                        jar.getEntry("weave.mod.json") ?: error("${modFile.name}} does not contain a weave.mod.json!")
                    )
                )

                config.mixins.forEach { Mixins.addConfiguration(it) }

                config.entrypoints.forEach {
                    val instance = classLoader.loadClass(it)
                        .getConstructor()
                        .newInstance() as? ModInitializer
                        ?: error("$it (mod ${modFile.name} does not implement ModInitializer!")

                    instance.preInit(hookManager)
                }
            }
    }

    private fun getOrCreateModDirectory(): Path {
        val dir = Paths.get(System.getProperty("user.home"), ".lunarclient", "mods")
        if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
        if (!dir.exists()) dir.createDirectory()
        return dir
    }
}

@Serializable
private data class WeaveModConfig(val mixins: List<String> = listOf(), val entrypoints: List<String>)
