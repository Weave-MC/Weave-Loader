package net.weavemc.loader.mixins

import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.gameClient
import net.weavemc.weave.api.gameVersion
import net.weavemc.weave.api.mapping.IMapper
import net.weavemc.weave.api.mapping.XSrgRemapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.logging.ILogger
import org.spongepowered.asm.logging.LoggerAdapterConsole
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.IMixinTransformer
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory
import org.spongepowered.asm.service.*
import org.spongepowered.asm.util.Constants
import org.spongepowered.asm.util.ReEntranceLock
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*

@Suppress("unused")
class DummyServiceBootstrap : IMixinServiceBootstrap {
    override fun getName() = "Weave Mixin Bootstrap"
    override fun getServiceClassName(): String = WeaveMixinService::class.java.name
    override fun bootstrap() {}
}

@Suppress("unused")
class DummyPropertyService : IGlobalPropertyService {
    private val properties = mutableMapOf<Key, Any?>()

    data class Key(val name: String) : IPropertyKey

    override fun resolveKey(name: String) = Key(name)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getProperty(key: IPropertyKey) = properties[key as Key] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getProperty(key: IPropertyKey, defaultValue: T) =
        properties[key as Key] as? T? ?: defaultValue

    override fun setProperty(key: IPropertyKey, value: Any?) {
        properties[key as Key] = value
    }

    override fun getPropertyString(key: IPropertyKey, defaultValue: String) = getProperty(key, defaultValue)
}

/**
 * Provides access to service layers which links Mixin transformers
 * to their particular host environment.
 *
 * @see IMixinService
 */
public class WeaveMixinService : IMixinService, IClassProvider, IClassBytecodeProvider {

    /**
     * Lock used to track transformer re-entrance
     * when co-operative loads and transformations are
     * performed by the service.
     *
     * @see [getReEntranceLock]
     */
    private val lock = ReEntranceLock(1)
    private val mapper: IMapper by lazy { XSrgRemapper(gameVersion, MixinEnvironment.getCurrentEnvironment().obfuscationContext ?: "notch") }

    private val genesisClassCache by lazy {
        if (gameClient == GameInfo.Client.LUNAR)
            this.javaClass.classLoader.javaClass
                .declaredFields
                .find { Map::class.java.isAssignableFrom(it.type) }!!
                .also { it.isAccessible = true }
                .get(this.javaClass.classLoader)
                as Map<String, ByteArray>
        else emptyMap()
    }

    /**
     * @return the name of the service.
     */
    override fun getName(): String = "Weave (from ${javaClass.classLoader})"

    /**
     * @return true if the service is valid for the current environment.
     */
    override fun isValid(): Boolean = true

    /**
     * Called at the subsystem's initialisation time.
     */
    override fun prepare() {}

    /**
     * @return the initial subsystem phase for this service.
     */
    override fun getInitialPhase(): MixinEnvironment.Phase = MixinEnvironment.Phase.DEFAULT

    /**
     * Called on an offer from the service provider, determines
     * whether to retain or ignore a component depending on its
     * own requirements.
     */
    override fun offer(internal: IMixinInternal) {
        if (internal is IMixinTransformerFactory) {
            (javaClass.classLoader as? SandboxedMixinLoader)?.state?.transformer = internal.createTransformer()
        }
    }

    /**
     * Called at the end of [prepare] to initialise a user-service.
     */
    override fun init() {}

    /**
     * Called at the start of any new phase.
     */
    override fun beginPhase() {}

    /**
     * Check whether the supplied object is a valid boot source
     * for a Mixin environment.
     *
     * @param bootSource The boot source object.
     */
    override fun checkEnv(bootSource: Any?) {}

    /**
     * @return The re-entrance lock for this service.
     *         Used to track re-entrance in co-operative loads.
     */
    override fun getReEntranceLock(): ReEntranceLock = lock

    /**
     * @return The class provider for this service.
     */
    override fun getClassProvider(): IClassProvider = this

    /**
     * @return The class bytecode provider for this service.
     */
    override fun getBytecodeProvider(): IClassBytecodeProvider = this

    /**
     * @return The transformer provider for this service.
     */
    override fun getTransformerProvider(): ITransformerProvider? = null

    /**
     * @return The class tracker for this service.
     */
    override fun getClassTracker(): IClassTracker? = null

    /**
     * @return The audit trail for this service.
     */
    override fun getAuditTrail(): IMixinAuditTrail? = null

    /**
     * @return Additional platform agents for this service.
     */
    override fun getPlatformAgents(): Collection<String> = emptyList()

    /**
     * @return The primary container for the current environment.
     *         This is usually a container storing Mixin classes,
     *         however, this can by another type of container if required.
     */
    override fun getPrimaryContainer(): IContainerHandle = ContainerHandleVirtual(name)

    /**
     * @return A collection of containers used in the current environment
     *         which contain un-processed Mixins.
     */
    override fun getMixinContainers(): Collection<IContainerHandle> = emptyList()

    /**
     * @param name The resource path.
     * @return A resource stream from the appropriate class loader.
     *         This is delegated via the service to choose the correct
     *         class loader to obtain a resource to stream.
     */
    override fun getResourceAsStream(name: String?): InputStream? =
        ClassLoader.getSystemResourceAsStream(name)

    /**
     * @return The detected side for the current environment.
     */
    override fun getSideName(): String = Constants.SIDE_CLIENT

    /**
     * @return The minimum compatibility level supported in the service.
     *         If the value is returned, it will be used as the minimum, and
     *         no lower levels will be supported.
     */
    override fun getMinCompatibilityLevel(): MixinEnvironment.CompatibilityLevel? = null

    /**
     * @return The maximum compatibility level supported in the service.
     *         If the value is returned, a warning will raise on any configuration
     *         attempts to SE a higher compatibility level.
     */
    override fun getMaxCompatibilityLevel(): MixinEnvironment.CompatibilityLevel? = null

    /**
     * **Implementations should be thread-safe since loggers may be
     * requested by threads other than the main application thread.**
     *
     * @param name The logger name.
     * @return A logger adapter with the specified ID.
     */
    override fun getLogger(name: String?): ILogger = LoggerAdapterConsole(name)

    /*
     * IClassProvider
     */

    /**
     * @return The class path of the current environment.
     */
    override fun getClassPath(): Array<URL> = emptyArray()

    /**
     * Finds the class in the service classloader.
     *
     * @param name The class name.
     * @return The resultant class.
     */
    override fun findClass(name: String?): Class<*> = Class.forName(name)

    /**
     * Marshal a call to [Class.forName] for a regular class.
     *
     * @param name The class name.
     * @param initialize Whether to initialize the class.
     * @return The resultant Klass.
     */
    override fun findClass(name: String?, initialize: Boolean): Class<*> =
        Class.forName(name, initialize, this.javaClass.classLoader)

    /**
     * Marshal a call to [Class.forName] for an agent class.
     *
     * @param name The class name.
     * @param initialize Whether to initialize the class.
     * @return The resultant Klass.
     */
    override fun findAgentClass(name: String?, initialize: Boolean): Class<*> =
        findClass(name, initialize)

    /*
     * IClassBytecodeProvider
     */

    /**
     * Retrieves a transformed class as an ASM tree.
     *
     * @param name The full class name.
     * @return The resultant class tree.
     */
    override fun getClassNode(name: String): ClassNode {
        val canonicalName = name.replace('/', '.')
        val internalName = name.replace('.', '/')

        try {
            val bytes = genesisClassCache[canonicalName]
                ?: getClassBytes(internalName)

            val cn = ClassNode()
            ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES)
            return cn
        } catch (ex: IOException) {
            throw ClassNotFoundException(canonicalName, ex)
        }
    }

    private fun getClassBytes(name: String): ByteArray {
        val name = (mapper.mapClass(name) ?: name).plus(".class")
        val stream = this.javaClass.classLoader.getResourceAsStream(name)
            ?: throw ClassNotFoundException("Failed to retrieve class bytes for $name")

        return stream.readBytes()
    }

    /**
     * Retrieves a transformed class as an ASM tree.
     *
     * @param name            The full class name.
     * @param runTransformers Whether to run transformers.
     * @return The resultant class tree.
     */
    override fun getClassNode(name: String, runTransformers: Boolean): ClassNode =
        getClassNode(name)
}

class SandboxedMixinLoader(private val parent: ClassLoader) : ClassLoader(parent) {
    val state = SandboxedMixinState(this)

    // FIXME: odd design choice for injecting resources
    private val injectedResources = mutableMapOf<String, ByteArray>()
    private val tempFiles = mutableMapOf<String, URL>()
    private val loaderExclusions = hashSetOf<String>()

    override fun findClass(name: String): Class<*> {
        // prevent infinite classloading loops
        if (name == javaClass.name) return javaClass

        return findLoadedClass(name) ?: if (shouldLoadParent(name)) parent.loadClass(name) else {
            val bytes = getResourceAsStream("${name.replace('.', '/')}.class")?.readBytes()
                ?: throw ClassNotFoundException("Could not sandbox mixin: resource unavailable: $name")

            val resultBytes = if (name == "org.spongepowered.asm.util.Constants") transformJava8Fix(bytes) else bytes
            defineClass(name, resultBytes, 0, resultBytes.size)
        }
    }

    private fun transformJava8Fix(bytes: ByteArray): ByteArray {
        val node = ClassNode().also { ClassReader(bytes).accept(it, 0) }

        val clinit = node.methods.search("<clinit>", "()V")
        val targetInsn = clinit.instructions.filterIsInstance<MethodInsnNode>()
            .first { it.owner == "java/lang/Package" && it.name == "getName" }

        clinit.instructions.insert(targetInsn, asm {
            pop
            ldc("org.spongepowered.asm.mixin")
        })

        clinit.instructions.remove(targetInsn)
        return ClassWriter(0).also { node.accept(it) }.toByteArray()
    }

    override fun loadClass(name: String) = findClass(name)

    private val systemClasses = setOf(
        "java.", "javax.", "org.xml.", "org.w3c.", "sun.", "jdk.",
        "com.sun.management.", "kotlin.", "kotlinx.", "org.objectweb.",
        "net.weavemc.loader.mixins.SandboxedMixinState"
    )

    private fun shouldLoadParent(name: String) = name !in loaderExclusions && systemClasses.any { name.startsWith(it) }

    fun injectResource(name: String, bytes: ByteArray) {
        injectedResources[name] = bytes
    }

    override fun getResource(name: String): URL? = tempFiles[name] ?: injectedResources[name]?.let {
        File.createTempFile("injected-resource", null).run {
            writeBytes(injectedResources.getValue(name))
            deleteOnExit()
            toURI().toURL().also { tempFiles[name] = it }
        }
    } ?: super.getResource(name)

    override fun getResources(name: String): Enumeration<URL> =
        if (name in injectedResources) Collections.enumeration(listOf(getResource(name)))
        else super.getResources(name)

    override fun getResourceAsStream(name: String): InputStream? =
        injectedResources[name]?.let { ByteArrayInputStream(it) } ?: super.getResourceAsStream(name)

    fun addLoaderExclusion(name: String) {
        loaderExclusions += name
    }
}

class SandboxedMixinState(private val loader: SandboxedMixinLoader) {
    // be careful to not load classes in the wrong context
    lateinit var transformer: Any

    // FIXME: hacky reflection: we really don't want to load classes in the wrong loader
    private val addMixin by lazy {
        loader.loadClass("org.spongepowered.asm.mixin.Mixins").getMethod("addConfiguration", String::class.java)
    }

    private val mixinEnvironment by lazy {
        loader.loadClass("org.spongepowered.asm.mixin.MixinEnvironment").getMethod("getDefaultEnvironment")(null)
    }

    private val transformMethod by lazy {
        transformer::class.java.getMethod(
            "transformClass",
            loader.loadClass("org.spongepowered.asm.mixin.MixinEnvironment"),
            String::class.java,
            ByteArray::class.java
        ).also { it.isAccessible = true }
    }

    var initialized = false
        private set

    fun initialize() {
        if (initialized) return

        injectService(
            "org.spongepowered.asm.service.IMixinService",
            "net.weavemc.loader.mixins.WeaveMixinService"
        )

        injectService(
            "org.spongepowered.asm.service.IMixinServiceBootstrap",
            "net.weavemc.loader.mixins.WeaveMixinServiceBootstrap"
        )

        injectService(
            "org.spongepowered.asm.service.IGlobalPropertyService",
            "net.weavemc.loader.mixins.DummyPropertyService"
        )

        loader.loadClass("org.spongepowered.asm.launch.MixinBootstrap").getMethod("init")(null)
        initialized = true
    }

    fun registerMixin(resourceName: String) {
        addMixin(null, resourceName)
    }

    private var dynamicMixinCounter = 0
        get() = field++

    fun transform(internalName: String, bytes: ByteArray) =
        transformMethod(transformer, mixinEnvironment, internalName, bytes) as ByteArray

    fun registerDynamicMixin(config: ByteArray) {
        val name = "dynamic-mixin$dynamicMixinCounter.json"
        loader.injectResource(name, config)
        registerMixin(name)
    }

    private fun injectService(name: String, value: String) {
        loader.injectResource("META-INF/services/$name", value.encodeToByteArray())
        loader.addLoaderExclusion(value)
    }
}
