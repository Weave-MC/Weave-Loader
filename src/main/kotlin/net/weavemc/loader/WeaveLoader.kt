package net.weavemc.loader

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.weavemc.loader.analytics.launchStart
import net.weavemc.loader.api.ModInitializer
import net.weavemc.loader.mixins.WeaveMixinService
import net.weavemc.loader.mixins.WeaveMixinTransformer
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.Mixins
import org.spongepowered.asm.service.MixinService
import java.lang.instrument.Instrumentation
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.*

/**
 * The main class of the Weave Loader.
 */
public object WeaveLoader {

    /**
     * Stores loaded mods for possible use later on.
     * The keys in this map are the mods' mod IDs.
     *
     * @see ModConfig
     */
    public val mods = mutableListOf<WeaveMod>()

    /**
     * This is where Weave loads mods, and [ModInitializer.preInit()][ModInitializer.preInit] is called.
     */
    @JvmStatic
    @OptIn(ExperimentalSerializationApi::class)
    public fun init(inst: Instrumentation) {
        println("[Weave] Initializing Weave")
        launchStart = System.currentTimeMillis()

        MixinBootstrap.init()
        check(MixinService.getService() is WeaveMixinService) { "Active mixin service is NOT WeaveMixinService" }

        inst.addTransformer(WeaveMixinTransformer)
        inst.addTransformer(HookManager)

        val json = Json { ignoreUnknownKeys = true }
        getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map { JarFile(it.toFile()).also(inst::appendToSystemClassLoaderSearch) }
            .forEach { jar ->
                println("[Weave] Loading ${jar.name}")

                val configEntry = jar.getEntry("weave.mod.json") ?: error("${jar.name} does not contain a weave.mod.json!")
                val config = json.decodeFromStream<ModConfig>(jar.getInputStream(configEntry))
                val name = config.name ?: jar.name.removeSuffix(".jar")

                config.mixinConfigs.forEach(Mixins::addConfiguration)
                HookManager.hooks += config.hooks.map(::instantiate)

                // TODO: Add a name field to the config.
                mods.add(WeaveMod(config.entrypoints.map(::instantiate), name.removeSuffix(".jar"), config))
            }

        /** Call preInit() once everything is done. */
        mods.forEach {
            it.instance.forEach(ModInitializer::preInit)
        }

        println("[Weave] Initialized Weave")
    }

    /**
     * The data class that is read from a mod's `weave.mod.json`.
     *
     * @property mixinConfigs The loaded mixin configs of the mod.
     * @property hooks The loaded hooks of the mod.
     * @property entrypoints The loaded [ModInitializer] entry points of the mod.
     * @property name The loaded name of the mod, if this field is not found, it will default to the mod's jar file.
     * @property modId The loaded mod ID of the mod, if this field is not found, it will be assigned
     *           a random placeholder value upon loading. **This value is not persistent between launches!**
     */
    @Serializable
    public data class ModConfig(
        val mixinConfigs: List<String> = listOf(),
        val hooks: List<String> = listOf(),
        val entrypoints: List<String>,
        val name: String? = null,
        val modId: String? = null
    )

    /**
     * Grabs the mods' directory, creating it if it doesn't exist.
     * **IF** the file exists as a file and not a directory, it will be deleted.
     *
     * @return The 'mods' directory: `"~/.weave/mods"`
     */
    private fun getOrCreateModDirectory(): Path {
        val dir = Paths.get(System.getProperty("user.home"), ".weave", "mods")
        if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
        if (!dir.exists()) dir.createDirectories()
        return dir
    }

    private inline fun<reified T> instantiate(className: String): T =
        Class.forName(className)
            .getConstructor()
            .newInstance() as? T
            ?: error("$className does not implement ${T::class.java.simpleName}!")
}


