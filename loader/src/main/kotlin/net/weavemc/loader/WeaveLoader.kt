package net.weavemc.loader

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.weavemc.loader.analytics.launchStart
import net.weavemc.loader.mixins.SandboxedMixinLoader
import net.weavemc.loader.mixins.WeaveMixinTransformer
import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.ModInitializer
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

/**
 * The main class of the Weave Loader.
 */
public object WeaveLoader {
    internal val mixinSandbox = SandboxedMixinLoader(javaClass.classLoader).state

    /**
     * Stored list of [WeaveMod]s.
     *
     * @see ModConfig
     */
    public val mods: MutableList<WeaveMod> = mutableListOf()

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
            mixinSandbox.initialize()
            inst.addTransformer(WeaveMixinTransformer)
        }.onFailure {
            System.err.println("Failed to load mixins:")
            it.printStackTrace()
        }

        /* Add as a backup search path (mainly used for resources) */
        modJars.forEach {
            inst.appendToSystemClassLoaderSearch(JarFile(it))
        }

        addMods(modJars)
        addApiHooks(apiJar)

        // Call preInit() once everything is done.
        mods.forEach { weaveMod ->
            weaveMod.config.entrypoints.forEach { entrypoint ->
                instantiate<ModInitializer>(entrypoint).preInit()
            }
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
        val entrypoints: List<String> = listOf(),
        val name: String? = null,
        val modId: String? = null
    )

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

            config.mixinConfigs.forEach { mixinSandbox.registerMixin(it) }
            HookManager.hooks += config.hooks.map(::instantiate)

            // TODO: Add a name field to the config.
            mods += WeaveMod(name, config)
        }
    }

    private inline fun <reified T> instantiate(className: String): T =
        Class.forName(className)
            .getConstructor()
            .newInstance() as? T
            ?: error("$className does not implement ${T::class.java.simpleName}!")
}
