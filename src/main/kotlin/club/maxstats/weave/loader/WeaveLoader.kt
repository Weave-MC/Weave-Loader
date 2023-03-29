package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import club.maxstats.weave.loader.mixins.WeaveMixinTransformer
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
    private lateinit var mods: List<Mod>

    /**
     * @see [club.maxstats.weave.loader.bootstrap.premain]
     */
    @JvmStatic
    @OptIn(ExperimentalSerializationApi::class)
    public fun preInit(inst: Instrumentation) {
        println("[Weave] Initializing Weave")

        inst.addTransformer(hookManager.Transformer())

        MixinBootstrap.init()
        inst.addTransformer(WeaveMixinTransformer)

        mods = getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map {
                val name = it.name
                val jar = JarFile(it.toFile())
                inst.appendToSystemClassLoaderSearch(jar)

                Mod(name, Json.decodeFromStream(jar.getInputStream(
                    jar.getEntry("weave.mod.json") ?: error("$name does not contain a weave.mod.json!")
                )))
            }

        mods.flatMap { it.config.mixinConfigs }.forEach {
            Mixins.addConfiguration(it)
        }

        mods.flatMap { it.config.hooks }.forEach {
            hookManager.register(
                Class.forName(it)
                    .getConstructor()
                    .newInstance() as? Hook
                    ?: error("$it does not implement Hook!")
            )
        }

        println("[Weave] Initialized Weave")
    }

    @JvmStatic
    public fun initMods() {
        println("[Weave] Initializing Mods")

        mods.flatMap { it.config.entrypoints }.forEach {
            val instance = Class.forName(it)
                .getConstructor()
                .newInstance() as? ModInitializer
                ?: error("$it does not implement ModInitializer!")

            instance.init()
        }

        println("[Weave] Initialized Mods")
    }

    private fun getOrCreateModDirectory(): Path {
        val dir = Paths.get(System.getProperty("user.home"), ".lunarclient", "mods")
        if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
        if (!dir.exists()) dir.createDirectory()
        return dir
    }

    private data class Mod(
        val name: String,
        val config: WeaveModConfig
    )

    @Serializable
    private data class WeaveModConfig(
        val mixinConfigs: List<String> = listOf(),
        val hooks: List<String> = listOf(),
        val entrypoints: List<String>
    )
}


