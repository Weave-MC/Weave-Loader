package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

                val config = Json.decodeFromString<WeaveModConfig>(
                    jar.getInputStream(
                        jar.getEntry("weave.mod.json") ?: error("No Weave mod config for $modFile!")
                    ).readBytes().decodeToString()
                )

                config.mixins
                    .filter { classLoader.getResourceAsStream(it) != null }
                    .forEach { Mixins.addConfiguration(it) }

                config.entrypoints.mapNotNull {
                    runCatching { classLoader.loadClass(it) }
                        .onFailure { println("Failed to load entry $it for $modFile, skipping...") }
                        .getOrNull()
                }.forEach {
                    (it.getConstructor().newInstance() as? ModInitializer
                        ?: error("$it (mod $modFile) does not implement ModInitializer!")).preInit(hookManager)
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