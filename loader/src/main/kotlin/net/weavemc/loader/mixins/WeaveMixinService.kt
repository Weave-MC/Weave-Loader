package net.weavemc.loader.mixins

import net.weavemc.loader.WeaveLoader
import net.weavemc.weave.api.mapping.LambdaAwareRemapper
import net.weavemc.weave.api.namedMapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.*
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.logging.ILogger
import org.spongepowered.asm.logging.LoggerAdapterConsole
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import org.spongepowered.asm.mixin.transformer.ClassInfo
import org.spongepowered.asm.mixin.transformer.IMixinTransformer
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory
import org.spongepowered.asm.service.*
import org.spongepowered.asm.util.Constants
import org.spongepowered.asm.util.ReEntranceLock
import java.io.ByteArrayInputStream
import java.io.File
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
class WeaveMixinService : IMixinService, IClassProvider, IClassBytecodeProvider {
    /**
     * Lock used to track transformer re-entrance
     * when co-operative loads and transformations are
     * performed by the service.
     *
     * @see [getReEntranceLock]
     */
    private val lock = ReEntranceLock(1)

    private val sandboxLoader get() = javaClass.classLoader as? SandboxedMixinLoader

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
            MixinEnvironment.getDefaultEnvironment().setOption(MixinEnvironment.Option.DEBUG_VERBOSE, true)
            sandboxLoader?.state?.transformer = internal.createTransformer()
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
        javaClass.classLoader.getResourceAsStream(name)

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
    @Deprecated("Deprecated in Java", ReplaceWith("emptyArray()"))
    override fun getClassPath(): Array<URL> = emptyArray()

    /**
     * Finds the class in the service classloader.
     *
     * @param name The class name.
     * @return The resultant class.
     */
    override fun findClass(name: String?): Class<*> = findClass(name, false)

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
    override fun getClassNode(name: String) = getClassNode(name, false)

    /**
     * Retrieves a transformed class as an ASM tree.
     *
     * @param name            The full class name.
     * @param runTransformers Whether to run transformers.
     * @return The resultant class tree.
     */
    override fun getClassNode(name: String, runTransformers: Boolean) = runCatching {
        ClassNode().also {
            ClassReader(sandboxLoader?.getClassBytes(name.replace('.', '/'), runTransformers))
                .accept(it, ClassReader.EXPAND_FRAMES)
        }
    }.onFailure { throw ClassNotFoundException(name.replace('/', '.'), it) }.getOrNull()
}

class SandboxedMixinLoader(private val parent: ClassLoader) : ClassLoader(parent) {
    private val injectedResources = mutableMapOf<String, ByteArray>()
    private val tempFiles = mutableMapOf<String, URL>()
    private val loaderExclusions = hashSetOf(
        "net.weavemc.loader.mixins.MixinAccess"
    )

    private val systemClasses = setOf(
        "java.", "javax.", "org.xml.", "org.w3c.", "sun.", "jdk.",
        "com.sun.management.", "kotlin.", "kotlinx.", "org.objectweb.",
        "net.weavemc.loader.mixins.SandboxedMixinState"
    )

    val state = SandboxedMixinState(this)

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

        val clinit = node.methods.first { it.name == "<clinit>" }
        val targetInsn = clinit.instructions.filterIsInstance<MethodInsnNode>()
            .first { it.owner == "java/lang/Package" && it.name == "getName" }

        // cannot use insn dsl due to relocate... life is sad
        clinit.instructions.insert(targetInsn, InsnList().apply {
            add(InsnNode(Opcodes.POP))
            add(LdcInsnNode("org.spongepowered.asm.mixin"))
        })

        clinit.instructions.remove(targetInsn)
        return ClassWriter(0).also { node.accept(it) }.toByteArray()
    }

    override fun loadClass(name: String) = findClass(name)

    private fun shouldLoadParent(name: String) = name in loaderExclusions || systemClasses.any { name.startsWith(it) }

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

    fun getClassBytes(name: String, isMixedIn: Boolean): ByteArray {
        val originalBytes = WeaveLoader.getClassBytes(name)
        return if (isMixedIn) remapMixedInClass(originalBytes, namedMapper) else originalBytes
    }

    fun remapMixedInClass(
        bytes: ByteArray,
        mapper: Remapper,
        visitor: (parent: ClassVisitor) -> ClassVisitor = { it }
    ): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(reader, 0)
        reader.accept(LambdaAwareRemapper(visitor(writer), mapper), 0)

        return writer.toByteArray()
    }
}

class SandboxedMixinState(private val loader: SandboxedMixinLoader) : MixinAccess by
    (loader.loadClass("net.weavemc.loader.mixins.MixinAccessImpl").getConstructor()
        .also { it.isAccessible = true }
        .newInstance() as MixinAccess) {
    // be careful to not load classes in the wrong context
    lateinit var transformer: Any

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
        if (loader.getResourceAsStream(resourceName)?.close() == null)
            error("Mixin config $resourceName does not exist!")

        addMixin(resourceName)
    }

    fun transform(internalName: String, bytes: ByteArray) = transform(transformer, internalName, bytes)

    private var dynamicMixinCounter = 0
        get() = field++

    fun registerDynamicMixin(config: ByteArray) {
        val name = "dynamic-mixin$dynamicMixinCounter.json"
        loader.injectResource(name, config)
        registerMixin(name)
    }

    private fun injectService(name: String, value: String) =
        loader.injectResource("META-INF/services/$name", value.encodeToByteArray())
}

interface MixinAccess {
    fun addMixin(name: String)
    fun transform(transformer: Any, internalName: String, bytes: ByteArray): ByteArray?
    fun getCommonSuperClass(a: String, b: String): String
}

@Suppress("unused")
private class MixinAccessImpl : MixinAccess {
    override fun addMixin(name: String) = Mixins.addConfiguration(name)
    override fun transform(transformer: Any, internalName: String, bytes: ByteArray): ByteArray? =
        (transformer as IMixinTransformer).transformClass(MixinEnvironment.getDefaultEnvironment(), internalName, bytes)

    override fun getCommonSuperClass(a: String, b: String): String = ClassInfo.getCommonSuperClass(a, b).name
}
