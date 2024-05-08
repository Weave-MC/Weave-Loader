package net.weavemc.loader

import com.grappenmaker.mappings.ClasspathLoaders
import com.grappenmaker.mappings.remappingNames
import me.xtrm.klog.Logger
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.internals.GameInfo
import net.weavemc.internals.MinecraftVersion
import net.weavemc.internals.ModConfig
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.mixin.SandboxedMixinLoader
import net.weavemc.loader.util.*
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

/**
 * The main class of the Weave Loader.
 */
class WeaveLoader(
    private val classLoader: URLClassLoaderAccessor,
    private val instrumentation: Instrumentation,
) {
    private val logger: Logger

    /**
     * Stored list of [WeaveMod]s.
     *
     * @see ModConfig
     */
    val mods: MutableList<WeaveMod> = mutableListOf()
    private val mixinInstances = mutableMapOf<String, SandboxedMixinLoader>()

    companion object {
        private var INSTANCE: WeaveLoader? = null

        @JvmStatic
        fun getInstance() = INSTANCE
            ?: fatalError("Attempted to retrieve WeaveLoader instance before it has been instantiated!")
    }

    init {
//        klogConfig {
//            defaultLevel = Level.INFO
//            appenders = mutableListOf(net.weavemcWeaveLogAppender(true))
//        }
        logger = Logger(WeaveLoader::class.qualifiedName!!)
        logger.info("Initializing Weave")

        INSTANCE = this
        launchStart = System.currentTimeMillis()
        instrumentation.addTransformer(InjectionHandler)

        finalize()
    }

    private fun finalize() {
        logger.trace("Finalizing Weave loading...")
        retrieveMods().forEach { it.registerAsMod() }
        logger.trace("Verifying dependencies")
        verifyDependencies()
        logger.trace("Populating mixin modifiers")
        populateMixinModifiers()

        logger.trace("Calling preInit() for mods")
        // TODO remove
        // Invoke preInit() once everything is done.
        mods.forEach { weaveMod ->
            weaveMod.config.entryPoints.forEach { entrypoint ->
                runCatching {
                    logger.debug("Calling $entrypoint#preInit")
                    instantiate<ModInitializer>(entrypoint)
                }.onFailure {
                    logger.error("Failed to instantiate $entrypoint#preInit", it)
                }.onSuccess {
                    runCatching {
                        @Suppress("DEPRECATION")
                        it.preInit(instrumentation)
                    }.onFailure {
                        logger.error("Exception thrown when invoking $entrypoint#preInit", it)
                    }
                }
            }
        }

        logger.info("Weave initialized in ${System.currentTimeMillis() - launchStart}ms")
        updateLaunchTimes()
    }

    /**
     * Invokes Weave Mods' init. @see net.weavemc.api.ModInitializer
     * Invoked at the head of Minecraft's main method. @see net.weavemc.loader.transformer.ModInitializerHook
     */
    fun initializeMods() {
        mods.forEach { weaveMod ->
            weaveMod.config.entryPoints.forEach { entrypoint ->
                runCatching {
                    instantiate<ModInitializer>(entrypoint)
                }.onFailure {
                    logger.error("Failed to instantiate $entrypoint#init", it)
                }.onSuccess {
                    runCatching {
                        it.init()
                    }.onFailure {
                        logger.error("Exception thrown when invoking $entrypoint#init", it)
                    }
                }
            }
        }
    }

    private fun verifyDependencies() {
        val duplicates = mods.groupingBy { it.modId }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) fatalError("Duplicate mods ${duplicates.joinToString()} were found")

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

    private fun mixinForNamespace(namespace: String) = mixinInstances.getOrPut(namespace) {
        logger.debug("Creating a new MixinLoader for namespace $namespace")
        val parent = classLoader.weaveBacking
        SandboxedMixinLoader(
            parent = parent,
            loader = ClasspathLoaders.fromLoader(parent)
                .remappingNames(MappingsHandler.mergedMappings.mappings, "official", namespace),
        ).apply { state.initialize() }
    }

    private fun populateMixinModifiers() {
        for (ns in MappingsHandler.mergedMappings.mappings.namespaces) {
            val state = mixinForNamespace(ns).state
            val targets = state.findTargets(state.transformer)
            if (targets.isEmpty()) continue

            val mapper = MappingsHandler.mapper(ns, MappingsHandler.environmentNamespace)
            InjectionHandler.registerModifier(object : Modifier {
                override val namespace = ns
                override val targets = targets.mapTo(hashSetOf()) { mapper.map(it.replace('.', '/')) }
                override fun apply(node: ClassNode, cfg: Hook.AssemblerConfig): ClassNode {
                    cfg.computeFrames()
                    return state.transform(node.name, node)
                }
            })
        }
    }

    /**
     * Registers mod's hooks and mixins then adds to mods list for later instantiation
     */
    private fun File.registerAsMod() {
        logger.trace("Registering ${this.name}")
        classLoader.addWeaveURL(this.toURI().toURL())

        JarFile(this).use { jar ->
            val config = jar.configOrFatal()
            val modId = config.modId

            // Added a backup classloader search for precautionary measures
            instrumentation.appendToSystemClassLoaderSearch(jar)

            config.hooks.forEach { hook ->
                logger.trace("Registering hook $hook")
                InjectionHandler.registerModifier(ModHook(config.namespace, instantiate(hook)))
            }

            val state = mixinForNamespace(config.namespace).state
            config.mixinConfigs.forEach { state.registerMixin(modId, it) }

            mods += WeaveMod(modId, config)
            logger.trace("Registered ${this.name}")
        }
    }

    private fun FileManager.ModJar.parseAndMap(): File {
        val fileName = file.name.substringBeforeLast('.')

        return JarFile(file).use {
            val config = it.configOrFatal()
            val compiledFor = config.compiledFor

            if (compiledFor != null && GameInfo.version != MinecraftVersion.fromVersionName(compiledFor)) {
                val extra = if (!isSpecific) {
                    " Hint: this mod was placed in the general mods folder. Consider putting mods in a version-specific mods folder"
                } else ""

                fatalError(
                    "Mod ${config.modId} was compiled for version $compiledFor, current version is ${GameInfo.version.versionName}.$extra"
                )
            }

            if (!MappingsHandler.isNamespaceAvailable(config.namespace)) {
                fatalError("Mod ${config.modId} was mapped in namespace ${config.namespace}, which is not available!")
            }

            file.createRemappedTemp(fileName, config)
        }
    }

    private fun retrieveMods() = FileManager.getMods().map { it.parseAndMap() }
}