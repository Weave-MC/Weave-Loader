package net.weavemc.loader.impl

import com.grappenmaker.mappings.*
import me.xtrm.klog.Logger
import net.weavemc.api.Hook
import net.weavemc.api.ModInitializer
import net.weavemc.internals.GameInfo
import net.weavemc.internals.ModConfig
import net.weavemc.loader.impl.bootstrap.PublicButInternal
import net.weavemc.loader.impl.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.impl.mixin.SandboxedMixinLoader
import net.weavemc.loader.impl.util.*
import org.apache.maven.model.building.DefaultModelBuilder
import org.apache.maven.model.building.ModelBuilder
import org.apache.maven.model.inheritance.DefaultInheritanceAssembler
import org.apache.maven.model.inheritance.InheritanceAssembler
import org.apache.maven.model.interpolation.ModelInterpolator
import org.apache.maven.model.interpolation.StringSearchModelInterpolator
import org.apache.maven.model.io.ModelReader
import org.apache.maven.model.normalization.DefaultModelNormalizer
import org.apache.maven.model.normalization.ModelNormalizer
import org.apache.maven.model.path.DefaultModelPathTranslator
import org.apache.maven.model.path.DefaultModelUrlNormalizer
import org.apache.maven.model.path.ModelPathTranslator
import org.apache.maven.model.path.ModelUrlNormalizer
import org.apache.maven.model.superpom.DefaultSuperPomProvider
import org.apache.maven.model.superpom.SuperPomProvider
import org.apache.maven.model.validation.DefaultModelValidator
import org.apache.maven.model.validation.ModelValidator
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader
import org.apache.maven.repository.internal.DefaultVersionRangeResolver
import org.apache.maven.repository.internal.DefaultVersionResolver
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.ArtifactDescriptorReader
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.impl.VersionRangeResolver
import org.eclipse.aether.impl.VersionResolver
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.lang.instrument.Instrumentation
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.jvm.java

/**
 * The main class of the Weave Loader.
 */
public class WeaveLoader(
    private val classLoader: URLClassLoaderAccessor,
    private val instrumentation: Instrumentation,
    private val mappedModJars: List<File>
) {
    private val logger = Logger(WeaveLoader::class.java.name)

    /**
     * Stored list of [WeaveMod]s.
     *
     * @see ModConfig
     */
    private val mods = mutableListOf(
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

    public companion object {
        private var INSTANCE: WeaveLoader? = null

        @JvmStatic
        public fun getInstance(): WeaveLoader = INSTANCE
            ?: fatalError("Attempted to retrieve WeaveLoader instance before it has been instantiated!")
    }

    /**
     * Called from [net.minecraft.client.main.Main.main].
     *
     * @see [net.weavemc.loader.impl.bootstrap.Bootstrap]
     */
    init {
        logger.info("Initializing Weave")

        INSTANCE = this
        launchStart = System.currentTimeMillis()
        instrumentation.addTransformer(InjectionHandler)
        addMinecraftApi()

        finalize()
    }

    private fun addMinecraftApi() {
        val localRepoPath by systemProperty(
            key = "weave.repo.local.path",
            defaultValue = Paths.get(System.getProperty("user.home"), ".weave", "maven-repository").toString()
        )
        val remoteRepoUrl by systemProperty(
            key = "weave.repo.remote.url",
            defaultValue = ""
        )

        val locator = MavenRepositorySystemUtils.newServiceLocator().apply {
            addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
            addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

            addService(ModelBuilder::class.java, DefaultModelBuilder::class.java)
            addService(ModelReader::class.java, org.apache.maven.model.io.DefaultModelReader::class.java)
            addService(ModelValidator::class.java, DefaultModelValidator::class.java)
            addService(ModelNormalizer::class.java, DefaultModelNormalizer::class.java)
            addService(ModelInterpolator::class.java, StringSearchModelInterpolator::class.java)
            addService(ModelPathTranslator::class.java, DefaultModelPathTranslator::class.java)
            addService(ModelUrlNormalizer::class.java, DefaultModelUrlNormalizer::class.java)
            addService(SuperPomProvider::class.java, DefaultSuperPomProvider::class.java)
            addService(InheritanceAssembler::class.java, DefaultInheritanceAssembler::class.java)

            // Maven Resolver (Aether) components
            addService(ArtifactDescriptorReader::class.java, DefaultArtifactDescriptorReader::class.java)
            addService(VersionResolver::class.java, DefaultVersionResolver::class.java)
            addService(VersionRangeResolver::class.java, DefaultVersionRangeResolver::class.java)

            setErrorHandler(object : DefaultServiceLocator.ErrorHandler() {
                override fun serviceCreationFailed(type: Class<*>?, impl: Class<*>?, exception: Throwable?) {
                    exception?.printStackTrace()
                }
            })
        }

        val system = locator.getService(RepositorySystem::class.java)
            ?: throw IllegalStateException("Could not initialize RepositorySystem")

        val session = MavenRepositorySystemUtils.newSession()
        session.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL

        // check local repo first
        val localRepo = LocalRepository(localRepoPath)

        session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)

        // in case it does not exist in the local repo
        // TODO: add proper repo
        val repo = RemoteRepository.Builder("weave-api-repo", "default", remoteRepoUrl)
            .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
            .build()

        val coords = "net.weavemc.api" +
                ":api-v${GameInfo.version.mappingName.replace('.', '_')}" +
                ":${weaveLoaderData["version"]}"
        val artifactRequest = ArtifactRequest().apply {
            artifact = DefaultArtifact(coords)
            repositories = listOf(repo)
        }

        logger.trace("Resolving Weave API ($coords)...")
        val result = system.resolveArtifact(session, artifactRequest)

        val apiFile = result.artifact.file

        apiFile
            .createRemappedTemp(
                name = "weave-api",
                fromNamespace = JarFile(apiFile).configOrFatal().namespace,
                suffix = "weaveapi"
            )
            .registerAsMod()
    }

    private fun finalize() {
        logger.trace("Finalizing Weave loading...")
        mappedModJars.forEach { it.registerAsMod() }
        logger.trace("Verifying dependencies")
        verifyDependencies()
        logger.trace("Populating mixin modifiers")
        populateMixinModifiers()
        logger.trace("Setting up access wideners")
        setupAccessWideners()

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
     * Invoked at the head of Minecraft's main method.
     *
     * @see [net.weavemc.loader.impl.bootstrap.transformer.ModInitializerHook]
     */
    @Suppress("unused")
    @PublicButInternal
    public fun initializeMods() {
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
        logger.debug("Creating a new SandboxedMixinLoader for namespace $namespace")
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
        }.reduceOrNull { acc, curr -> acc + curr }?.toTree() ?: return

        InjectionHandler.registerModifier(object : Modifier {
            override val namespace = MappingsHandler.environmentNamespace
            override val targets = tree.classes.mapTo(hashSetOf()) { it.key }

            override fun apply(node: ClassNode, cfg: Hook.AssemblerConfig) = node.applyWidener(tree)
        })
    }

    /**
     * Registers mod's hooks and mixins then adds to mods list for later instantiation
     */
    public fun File.registerAsMod() {
        logger.trace("Registering mod $name")
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
            logger.trace("Registered mod $name")
        }
    }
}