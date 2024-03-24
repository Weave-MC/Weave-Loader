package net.weavemc.loader

import kotlinx.serialization.json.Json
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.internals.GameInfo
import net.weavemc.loader.analytics.launchStart
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.injection.InjectionHandler
import net.weavemc.loader.injection.ModHook
import net.weavemc.loader.mapping.MappingsHandler
import net.weavemc.loader.mixin.SandboxedMixinLoader
import net.weavemc.loader.mixin.SandboxedMixinState
import net.weavemc.loader.mixin.SandboxedMixinTransformer
import net.weavemc.loader.util.FileManager
import net.weavemc.loader.util.JSON
import net.weavemc.loader.util.ModConfig
import net.weavemc.loader.util.WeaveMod
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile
import javax.swing.JOptionPane
import kotlin.system.exitProcess

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
    private val mixinLoader = SandboxedMixinLoader()
    private val mixinState = mixinLoader.state

    init {
        println("[Weave] Initializing Weave")
        launchStart = System.currentTimeMillis()
        instrumentation.addTransformer(InjectionHandler)

        loadAndInitMods()
    }

    private fun fatalError(message: String): Nothing {
        JOptionPane.showMessageDialog(
            /* parentComponent = */ null,
            /* message = */ "An error occurred: $message",
            /* title = */ "Weave Loader error",
            /* messageType = */ JOptionPane.ERROR_MESSAGE
        )

        exitProcess(-1)
    }

    private fun verifyDependencies() {
        // TODO: jank
        val duplicates = mods.groupingBy { it.modId }.eachCount().filterValues { it > 1 }.keys
        if(duplicates.isNotEmpty()) fatalError("Duplicate mods ${duplicates.joinToString()} were found")

        val dependencyGraph = mods.associate { it.modId to it.config.dependencies }
        val seen = hashSetOf<String>()
        dependencyGraph.keys.forEach { toDetermine ->
            // soFar = List to keep order
            // Supposedly the list will be small enough such that a linear search is efficient enough
            fun verify(curr: String, soFar: List<String>) {
                if (curr in soFar) fatalError(
                    "Circular dependency: $toDetermine's dependency graph eventually " +
                            "meets $curr again through ${(soFar + curr).joinToString(" -> ")}"
                )

                if (!seen.add(curr)) return
                val deps = dependencyGraph[curr] ?: fatalError("Dependency $curr for mod $toDetermine is not available")
                deps.forEach { verify(it, soFar + curr) }
            }

            verify(toDetermine, emptyList())
        }
    }

    @Mixin(SomeTest::class)
    class MixinTest {
        @Inject(at = [At("HEAD")], method = ["test"])
        fun test(ci: CallbackInfo) {
            println("Hello mixin poc!")
        }
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

        mixinState.initialize()
        mappedMods.forEach { it.registerAsMod() }
        verifyDependencies()

        mixinState.registerSpoofedMixin("net/weavemc/loader/WeaveLoader\$MixinTest")
        instrumentation.addTransformer(SandboxedMixinTransformer(mixinState))

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
            jar.entries().asSequence()
                .filter { it.name.startsWith("net/weavemc/api/hooks/") && !it.isDirectory }
                .forEach {
                    runCatching {
                        val hookClass = Class.forName(it.name.removeSuffix(".class").replace('/', '.'))
                        if (hookClass.superclass == Hook::class.java) {
                            val namespace =
                                if (GameInfo.gameVersion.protocol >= GameInfo.MinecraftVersion.V1_16_5.protocol)
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

            config.mixinConfigs.forEach { mixinState.registerMixin(it) }

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

    // TODO: rethrow errors
    return json.decodeFromString<ModConfig>(getInputStream(configEntry).readBytes().decodeToString())
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