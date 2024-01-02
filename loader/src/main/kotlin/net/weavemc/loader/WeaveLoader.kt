package net.weavemc.loader

import kotlinx.serialization.json.Json
import net.weavemc.loader.analytics.launchStart
import net.weavemc.loader.mixins.MixinApplicator
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.mapping.MappingsHandler
import net.weavemc.loader.util.*
import net.weavemc.loader.util.FileManager
import net.weavemc.loader.util.JSON
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
    fun init(inst: Instrumentation, clAccessor: URLClassLoaderAccessor) {
        println("[Weave] Initializing Weave")
        launchStart = System.currentTimeMillis()
        // Add common api
        clAccessor.addWeaveURL(FileManager.getCommonApi().toURI().toURL())

        inst.addTransformer(HookManager)

        runCatching {
            mixins = MixinApplicator()
            inst.addTransformer(mixins.Transformer())
        }.onFailure {
            System.err.println("[Weave] Failed to load mixins:")
            it.printStackTrace()
        }

        val (mappedMods, mappedApi) = retrieveModsAndApi()

        if (mappedApi != null) {
            clAccessor.addWeaveURL(mappedApi.toURI().toURL())
            registerApi(mappedApi)
        }

        mappedMods.forEach {
            clAccessor.addWeaveURL(it.toURI().toURL())
            /* Add as a backup search path (mainly used for resources) */
            inst.appendToSystemClassLoaderSearch(JarFile(it))

            registerMod(it)
        }

        mixins.freeze()

        // Call preInit() once everything is done.
        mods.forEach { weaveMod ->
            weaveMod.config.entrypoints.forEach { entrypoint ->
                instantiate<ModInitializer>(entrypoint).preInit(inst)
            }
        }

        println("[Weave] Initialized Weave")
    }

    fun retrieveModsAndApi(): Pair<List<File>, File?> {
        fun File.createRemappedTemp(name: String): File {
            val temp = File.createTempFile(name, "-weavemod.jar")
            MappingsHandler.remapModJar(
                mappings = MappingsHandler.mergedMappings.mappings,
                input = this,
                output = temp,
                classpath = listOf(FileManager.getVanillaMinecraftJar())
            )
            temp.deleteOnExit()
            return temp
        }

        val versionApi = FileManager.getVersionApi()
        val mods = FileManager.getMods().map { it.file }

        val mappedVersionApi = versionApi?.createRemappedTemp("version-api")
        val mappedMods = mods.map { it.createRemappedTemp(it.nameWithoutExtension) }

        return mappedMods to mappedVersionApi
    }

    /**
     * Adds hooks for Weave events, corresponding to the Minecraft version
     */
    private fun registerApi(api: File) {
        println("[Weave] Loading API hooks")
        val apiJar = JarFile(api)
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
    private fun registerMod(mod: File) {
        println("[Weave] Registering ${mod.name}")

        val config = mod.fetchModConfig(JSON)
        val name = config.name ?: mod.name.removeSuffix(".jar")

        JarFile(mod).use { jar ->
            config.mixinConfigs.forEach { mixins.registerMixin(it, jar, MappingsHandler.environmentRemapper) }
        }
        HookManager.hooks += config.hooks.map { ModHook(instantiate(it)) }

        // TODO: Add a name field to the config.
        mods += WeaveMod(name, config)
        println("[Weave] Registered ${mod.name}")
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
