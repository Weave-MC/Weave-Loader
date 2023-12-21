package net.weavemc.loader

import kotlinx.serialization.json.Json
import net.weavemc.loader.analytics.launchStart
import net.weavemc.loader.mixins.MixinApplicator
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.loader.mapping.MappingsHandler
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

/**
 * The main class of the Weave Loader.
 */
object WeaveLoader {

    /**
     * Stored list of [WeaveMod]s.
     *
     * @see ModConfig
     */
    val mods: MutableList<WeaveMod> = mutableListOf()
    lateinit var mixins: MixinApplicator

    /**
     * This is where Weave loads mods, and [ModInitializer.preInit] is called.
     *
     * @see net.weavemc.loader.bootstrap.premain
     */
    @JvmStatic
    fun init(inst: Instrumentation, apiJar: File?, modJars: List<File>) {
        println("[Weave] Initializing Weave")
        launchStart = System.currentTimeMillis()
        inst.addTransformer(HookManager)

        runCatching {
            mixins = MixinApplicator()
            inst.addTransformer(mixins.Transformer())
        }.onFailure {
            System.err.println("[Weave] Failed to load mixins:")
            it.printStackTrace()
        }

        /* Add as a backup search path (mainly used for resources) */
        modJars.forEach { inst.appendToSystemClassLoaderSearch(JarFile(it)) }

        if (apiJar != null)
            addApiHooks(apiJar)
        addMods(modJars)

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
        println("[Weave] Loading API hooks")
        val apiJar = JarFile(apiFile)
        apiJar.entries()
            .toList()
            .filter { it.name.startsWith("net/weavemc/api/hooks/") && !it.isDirectory }
            .forEach {
                runCatching {
                    val clazz = Class.forName(it.name.removeSuffix(".class").replace('/', '.'))
                    if (clazz.superclass == Hook::class.java) {
                        HookManager.hooks += ModHook(clazz.getConstructor().newInstance() as Hook)
                    }
                }
            }
    }

    /**
     * Adds Weave Mod's Hooks to HookManager and adds to mods list for later instantiation
     */
    private fun addMods(modJars: List<File>) {
        modJars.forEach { file ->
            println("[Weave] Loading ${file.name}")

            val config = file.fetchModConfig(JSON)
            val name = config.name ?: file.name.removeSuffix(".jar")

            JarFile(file).use { jar ->
                config.mixinConfigs.forEach { mixins.registerMixin(it, jar, MappingsHandler.environmentRemapper) }
            }
            HookManager.hooks += config.hooks.map { ModHook(instantiate(it)) }

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

internal fun File.fetchModConfig(json: Json) = JarFile(this).use {
    val configEntry = it.getEntry("weave.mod.json")
        ?: error("${it.name} does not contain a weave.mod.json!")

    json.decodeFromString<ModConfig>(it.getInputStream(configEntry).readBytes().decodeToString())
}

internal fun JarFile.fetchMixinConfig(path: String, json: Json): MixinConfig {
    val configEntry = getEntry(path) ?: error("$name does not contain a $path (the mixin config)!")
    return json.decodeFromString<MixinConfig>(getInputStream(configEntry).readBytes().decodeToString())
}
