package net.weavemc.loader

import kotlinx.serialization.json.Json
import net.weavemc.loader.analytics.launchStart
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.internals.GameInfo
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.injection.InjectionHandler
import net.weavemc.loader.injection.ModHook
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
open class WeaveLoader(
    val classLoader: URLClassLoaderAccessor,
    val instrumentation: Instrumentation
) {
    /**
     * Stored list of [WeaveMod]s.
     *
     * @see ModConfig
     */
    private val mods: MutableList<WeaveMod> = mutableListOf()

    init {
        println("[Weave] Initializing Weave")
        launchStart = System.currentTimeMillis()
        instrumentation.addTransformer(InjectionHandler)

        loadAndInitMods()
    }

    /**
     * This is where Weave loads mods, api(s), and [ModInitializer.preInit] is called.
     *
     * @see net.weavemc.loader.bootstrap.premain
     */
    private fun loadAndInitMods() {
        // Add common api
        classLoader.addWeaveURL(FileManager.getCommonApi().toURI().toURL())

        val (mappedMods, mappedApi) = retrieveModsAndApi()

        mappedApi?.registerAsAPI()

        mappedMods.forEach { mod ->
            mod.registerAsMod()
        }

        // Invoke preInit() once everything is done.
        mods.forEach { weaveMod ->
            weaveMod.config.entryPoints.forEach { entrypoint ->
                instantiate<ModInitializer>(entrypoint).preInit(instrumentation)
            }
        }

        println("[Weave] Initialized Weave")
    }


    /**
     * Adds hooks for Weave events, corresponding to the Minecraft version
     */
    private fun File.registerAsAPI() {
        println("[Weave] Loading API hooks")
        classLoader.addWeaveURL(this.toURI().toURL())

        JarFile(this).use { jar ->
            jar.entries()
                .toList()
                .filter { it.name.startsWith("net/weavemc/api/hooks/") && !it.isDirectory }
                .forEach {
                    runCatching {
                        val hookClass = Class.forName(it.name.removeSuffix(".class").replace('/', '.'))
                        if (hookClass.superclass == Hook::class.java) {
                            val namespace = if (GameInfo.gameVersion.protocol >= GameInfo.MinecraftVersion.V1_16_5.protocol)
                                "mojmap"
                            else
                                "mcp"

                            InjectionHandler.registerModifier(
                                ModHook(namespace, hookClass.getConstructor().newInstance() as Hook)
                            )
                        }
                    }
                }
        }
    }

    /**
     * Registers mod's hooks and mixins then adds to mods list for later instantiation
     */
    private fun File.registerAsMod() {
        println("[Weave] Registering ${this.name}")
        classLoader.addWeaveURL(this.toURI().toURL())

        JarFile(this).use { jar ->
            val config = jar.fetchModConfig(JSON)
            val modId = config.modId
            // Added a backup classloader search for precautionary measures
            instrumentation.appendToSystemClassLoaderSearch(jar)

            config.hooks.forEach { hook ->
                InjectionHandler.registerModifier(ModHook(config.namespace, instantiate(hook)))
            }
            // TODO register the mixins

            mods += WeaveMod(modId, config)
            println("[Weave] Registered ${this.name}")
        }
    }

    private fun File.parseAndMap(): File {
        val fileName = this.name.substringBeforeLast('.')

        JarFile(this).use {
            val config = it.fetchModConfig(JSON)
            return createRemappedTemp(fileName, config)
        }
    }

    private fun retrieveModsAndApi(): Pair<List<File>, File?> {
        val versionApi = FileManager.getVersionApi()
        val mods = FileManager.getMods().map { it.file }

        val mappedVersionApi = versionApi?.parseAndMap()
        val mappedMods = mods.map { it.parseAndMap() }

        return mappedMods to mappedVersionApi
    }

    private inline fun <reified T> instantiate(className: String): T =
        Class.forName(className)
            .getConstructor()
            .newInstance() as? T
            ?: error("$className does not implement ${T::class.java.simpleName}!")
}

private fun JarFile.fetchModConfig(json: Json): ModConfig {
    val configEntry = this.getEntry("weave.mod.json")
        ?: error("${this.name} does not contain a weave.mod.json!")

    return json.decodeFromString<ModConfig>(this.getInputStream(configEntry).readBytes().decodeToString())
}

private fun JarFile.fetchMixinConfig(path: String, json: Json): MixinConfig {
    val configEntry = getEntry(path) ?: error("$name does not contain a $path (the mixin config)!")
    return json.decodeFromString<MixinConfig>(getInputStream(configEntry).readBytes().decodeToString())
}

private fun File.createRemappedTemp(name: String, config: ModConfig): File {
    val temp = File.createTempFile(name, "-weavemod.jar")
    MappingsHandler.remapModJar(
        mappings = MappingsHandler.mergedMappings.mappings,
        input = this,
        output = temp,
        classpath = listOf(FileManager.getVanillaMinecraftJar()),
        from = config.namespace
    )
    temp.deleteOnExit()
    return temp
}
