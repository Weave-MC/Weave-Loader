package net.weavemc.loader

import com.grappenmaker.mappings.*
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.internals.GameInfo
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
    private val mappedModJars: List<File>
) {
    /**
     * Stored list of [WeaveMod]s.
     *
     * @see ModConfig
     */
    val mods: MutableList<WeaveMod> = mutableListOf(
        // Fake base game mod so dependencies can be made on them
        // TODO: proper versioned dependencies, eg. optional, version greater/less than...
        WeaveMod(
            modId = "minecraft", config = ModConfig(
                name = "Minecraft",
                modId = "minecraft",
                entryPoints = emptyList(),
                mixinConfigs = emptyList(),
                hooks = emptyList(),
                tweakers = emptyList(),
                namespace = MappingsHandler.environmentNamespace,
                dependencies = emptyList(),
                compiledFor = GameInfo.version.versionName
            )
        )
    )

    private val mixinInstances = mutableMapOf<String, SandboxedMixinLoader>()

    companion object {
        private var INSTANCE: WeaveLoader? = null

        @JvmStatic
        fun getInstance() =
            INSTANCE ?: fatalError("Attempted to retrieve WeaveLoader instance before it has been instantiated!")
    }

    init {
        println("[Weave] Initializing Weave")

        INSTANCE = this
        launchStart = System.currentTimeMillis()
        instrumentation.addTransformer(InjectionHandler)

        finalize()
    }

    private fun finalize() {
        println("[Weave] Finalizing Weave")
        mappedModJars.forEach { it.registerAsMod() }
        println("[Weave] Verifying dependencies")
        verifyDependencies()
        println("[Weave] Populating mixin modifiers")
        populateMixinModifiers()
        println("[Weave] Setting up access wideners")
        setupAccessWideners()

        println("[Weave] calling preInit() for mods")
        // TODO remove
        // Invoke preInit() once everything is done.
        mods.forEach { weaveMod ->
            weaveMod.config.entryPoints.forEach { entrypoint ->
                runCatching {
                    instantiate<ModInitializer>(entrypoint)
                }.onFailure {
                    it.printStackTrace()
                    println("Failed to instantiate $entrypoint#preInit")
                }.onSuccess {
                    runCatching {
                        @Suppress("DEPRECATION")
                        it.preInit(instrumentation)
                    }.onFailure {
                        it.printStackTrace()
                        println("Exception thrown when invoking $entrypoint#preInit")
                    }
                }
            }
        }

        println("[Weave] Initialized Weave")
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
                    it.printStackTrace()
                    println("Failed to instantiate $entrypoint#init")
                }.onSuccess {
                    runCatching {
                        it.init()
                    }.onFailure {
                        it.printStackTrace()
                        println("Exception thrown when invoking $entrypoint#init")
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
        val parent = classLoader.backing
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
                override fun apply(node: ClassNode, cfg: Hook.AssemblerConfig) {
                    cfg.computeFrames()
                    state.transform(node.name, node)
                }
            })
        }
    }

    private fun setupAccessWideners() {
        val tree = mods.asSequence().flatMap { it.config.accessWideners }.mapNotNull { aw ->
            val res = javaClass.classLoader.getResourceAsStream(aw) ?: return@mapNotNull let {
                println("[Weave] Could not load access widener configuration $aw")
                null
            }

            loadAccessWidener(res.readBytes().decodeToString().trim().lines())
                .remap(MappingsHandler.mergedMappings.mappings, MappingsHandler.environmentNamespace)
        }.join().toTree()

        InjectionHandler.registerModifier(object : Modifier {
            override val namespace = MappingsHandler.environmentNamespace
            override val targets = tree.classes.mapTo(hashSetOf()) { it.key }

            override fun apply(node: ClassNode, cfg: Hook.AssemblerConfig) = node.applyWidener(tree)
        })
    }

    /**
     * Registers mod's hooks and mixins then adds to mods list for later instantiation
     */
    private fun File.registerAsMod() {
        println("[Weave] Registering ${this.name}")
        classLoader.addWeaveURL(this.toURI().toURL())

        JarFile(this).use { jar ->
            val config = jar.configOrFatal()
            val modId = config.modId

            // Added a backup classloader search for precautionary measures
            instrumentation.appendToSystemClassLoaderSearch(jar)

            config.hooks.forEach { hook ->
                println("[Weave] Registering hook $hook")
                InjectionHandler.registerModifier(ModHook(config.namespace, instantiate(hook)))
            }

            val state = mixinForNamespace(config.namespace).state
            config.mixinConfigs.forEach { state.registerMixin(modId, it) }

            mods += WeaveMod(modId, config)
            println("[Weave] Registered ${this.name}")
        }
    }
}