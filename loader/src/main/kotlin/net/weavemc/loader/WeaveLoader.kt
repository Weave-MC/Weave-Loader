package net.weavemc.loader

import kotlinx.serialization.json.Json
import net.weavemc.loader.analytics.launchStart
import net.weavemc.loader.mixins.MixinApplicator
import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.ModInitializer
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

/**
 * The main class of the Weave Loader.
 */
public object WeaveLoader {

    /**
     * Stored list of [WeaveMod]s.
     *
     * @see ModConfig
     */
    public val mods: MutableList<WeaveMod> = mutableListOf()
    lateinit var mixins: MixinApplicator

    /**
     * This is where Weave loads mods, and [ModInitializer.preInit] is called.
     *
     * @see net.weavemc.loader.bootstrap.premain
     */
    @JvmStatic
    public fun init(inst: Instrumentation, apiJar: File, modJars: List<File>) {
        println("[Weave] Initializing Weave")
        launchStart = System.currentTimeMillis()
        inst.addTransformer(HookManager)

        runCatching {
            mixins = MixinApplicator()
            inst.addTransformer(mixins.Transformer())
        }.onFailure {
            System.err.println("Failed to load mixins:")
            it.printStackTrace()
        }

        /* Add as a backup search path (mainly used for resources) */
        modJars.forEach { inst.appendToSystemClassLoaderSearch(JarFile(it)) }

        addMods(modJars)
        addApiHooks(apiJar)

        // Call preInit() once everything is done.
        mods.forEach { weaveMod ->
            weaveMod.config.entrypoints.forEach { entrypoint ->
                instantiate<ModInitializer>(entrypoint).preInit(inst)
            }
        }

        println("[Weave] Initialized Weave")
    }

    /**
     * Adds hooks for Weave events, corresponding to the Minecraft version
     */
    private fun addApiHooks(apiFile: File) {
        val apiJar = JarFile(apiFile)
        apiJar.entries()
            .toList()
            .filter { it.name.startsWith("net/weavemc/weave/api/hooks/") && !it.isDirectory }
            .forEach {
                runCatching {
                    val clazz = Class.forName(it.name.removeSuffix(".class").replace('/', '.'))
                    if (clazz.superclass == Hook::class.java) {
                        HookManager.hooks += clazz.getConstructor().newInstance() as Hook
                    }
                }
            }
    }

    /**
     * Adds Weave Mod's Hooks to HookManager and adds to mods list for later instantiation
     */
    private fun addMods(modJars: List<File>) {
        val json = Json { ignoreUnknownKeys = true }

        modJars.forEach { file ->
            val jar = JarFile(file)
            println("[Weave] Loading ${jar.name}")

            val configEntry = jar.getEntry("weave.mod.json")
                ?: error("${jar.name} does not contain a weave.mod.json!")

            val config = json.decodeFromString<ModConfig>(jar.getInputStream(configEntry).readBytes().decodeToString())
            val name = config.name ?: jar.name.removeSuffix(".jar")

            config.mixinConfigs.forEach { mixins.registerMixin(it, jar) }
            HookManager.hooks += config.hooks.map(::instantiate)

            // TODO: Add a name field to the config.
            mods += WeaveMod(name, config)
        }

        mixins.freeze()
    }

    private inline fun <reified T> instantiate(className: String): T =
        Class.forName(className)
            .getConstructor()
            .newInstance() as? T
            ?: error("$className does not implement ${T::class.java.simpleName}!")
}
