package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import club.maxstats.weave.loader.api.WeavePhase
import club.maxstats.weave.loader.api.annotation.Entry
import club.maxstats.weave.loader.mixins.service.WeaveMixinTransformer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.Mixins
import java.lang.instrument.Instrumentation
import java.lang.reflect.Method
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
        Mixins.addConfiguration("weave.mixin.json")

        inst.addTransformer(WeaveMixinTransformer)

        mods = getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map { modPath ->
                val jarFile = JarFile(modPath.toFile())

                inst.appendToSystemClassLoaderSearch(jarFile)

                val config = Json.decodeFromStream<WeaveModConfig>(jarFile.getInputStream(jarFile.getEntry("weave.mod.json")))
                val mod = Mod(modPath.fileName.toString(), config)

                mod.entryPoints.addAll(config.entrypoints
                    .map {
                        getEntryPointMethods(ModEntryPoint(Class.forName(it)))
                    })

                config.mixinConfigs.forEach { Mixins.addConfiguration(it) }
                config.hooks.forEach {
                    val hookClazz = Class.forName(it)
                    val hook = hookClazz
                        .getConstructor()
                        .newInstance() as? Hook
                        ?: error("$it does not extend ${Hook::class.java.name}")

                    hookManager.register(hook)
                }

                mod
            }

        switchPhase(WeavePhase.PRE_INIT)

        println("[Weave] Initialized Weave")
    }

    @JvmStatic
    public fun initLegacyMods() {
        println("[Weave] Initializing legacy mods")

        mods
            .flatMap { it.entryPoints }
            .filter { it.legacy }
            .forEach {
                val instance = it.clazz
                    .getConstructor()
                    .newInstance() as? ModInitializer
                    ?: error("$it does not implement ModInitializer!")

                instance.init()
            }

        println("[Weave] Initialized legacy mods")
    }

    @JvmStatic
    public fun switchPhase(phase: WeavePhase) {
        println("[Weave] Switched into the " + phase.name + " phase")

        for (mod in mods) {
            mod.entryPoints
                .filter { !it.legacy }
                .map {
                    if (it.instance == null) {
                        it.instance = it.clazz
                            .getConstructor()
                            .newInstance()
                    }

                    it
                }
                .forEach { entry ->
                    entry.methods
                        .filter {
                            it.second == phase
                        }
                        .forEach {
                            it.first.invoke(entry.instance)
                        }
                }
        }
    }

    private fun getOrCreateModDirectory(): Path {
        val dir = Paths.get(System.getProperty("user.home"), ".lunarclient", "mods")

        if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
        if (!dir.exists()) dir.createDirectory()

        return dir
    }

    private fun getEntryPointMethods(entryPoint: ModEntryPoint): ModEntryPoint {
        // class + jazz = clazz (class that listens to jazz)!!
        val clazz = entryPoint.clazz

        clazz.methods
            .mapNotNull {
                val annotation = it.getAnnotation(Entry::class.java)

                if (annotation != null) { Pair(it, annotation) } else { null }
            }
            .sortedBy { it.second.priority }
            .forEach { entryPoint.methods.add(Pair(it.first, it.second.phase)) }

        return entryPoint
    }

    private data class Mod(
        val name: String,
        val config: WeaveModConfig,
        val entryPoints: MutableList<ModEntryPoint> = mutableListOf()
    )

    private data class ModEntryPoint(
        val clazz: Class<*>,

        var instance: Any? = null,
        var legacy: Boolean = false,

        val methods: MutableList<Pair<Method, WeavePhase>> = mutableListOf()
    )

    @Serializable
    private data class WeaveModConfig(
        val mixinConfigs: List<String> = listOf(),
        val hooks: List<String> = listOf(),
        val entrypoints: List<String>
    )
}


