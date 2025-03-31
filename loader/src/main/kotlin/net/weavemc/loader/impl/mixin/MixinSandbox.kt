package net.weavemc.loader.impl.mixin

import com.grappenmaker.mappings.ClasspathLoaders
import me.xtrm.klog.dsl.klog
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import net.weavemc.loader.impl.util.asClassNode
import net.weavemc.loader.impl.util.asClassReader
import net.weavemc.loader.impl.util.illegalToReload
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.MixinEnvironment.Phase
import org.spongepowered.asm.mixin.Mixins
import org.spongepowered.asm.mixin.extensibility.IMixinConfig
import org.spongepowered.asm.mixin.transformer.IMixinTransformer
import org.spongepowered.asm.service.MixinService
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.lang.instrument.ClassFileTransformer
import java.net.URL
import java.security.ProtectionDomain
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val logger by klog

/**
 * Implements a [ClassFileTransformer] that passes all loaded classes through the wrapped [mixin] state
 */
public class SandboxedMixinTransformer(
    private val mixin: SandboxedMixinState
) : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray? = mixin.transformOrNull(className, classfileBuffer)
}

/**
 * A [ClassLoader] that is capable of "sandboxing" Spongepowered Mixin. The [parent] loader can be specified,
 * which will be used to load non-sandboxed classes. A [loader] can be specified, which is responsible for providing
 * class bytes by a given name (forward slashes are the package separator).
 */
public class SandboxedMixinLoader(
    private val parent: ClassLoader = getSystemClassLoader(),
    private val loader: (name: String) -> ByteArray? = ClasspathLoaders.fromLoader(parent),
) : ClassLoader(parent) {
    private val injectedResources = mutableMapOf<String, ByteArray>()
    private val tempFiles = mutableMapOf<String, URL>()
    private val loaderExclusions = mutableSetOf("net.weavemc.loader.impl.mixin.MixinAccess")

    private val systemClasses = illegalToReload + setOf(
        "kotlin.", "kotlinx.", "org.objectweb.asm.",
        "me.xtrm.klog.", "net.weavemc.loader.impl.mixin.SandboxedMixinState"
    )

    /**
     * Responsible for managing the state of the sandboxed mixin environment
     */
    public val state: SandboxedMixinState = SandboxedMixinState(this)

    private fun transform(internalName: String, bytes: ByteArray): ByteArray? {
        if (internalName != "org/spongepowered/asm/util/Constants") return null

        val reader = bytes.asClassReader()
        val node = reader.asClassNode()
        require(node.name == internalName)

        val target = node.methods.named("<clinit>")
        val call = target.instructions.filterIsInstance<MethodInsnNode>()
            .first { it.name == "getName" && it.owner == internalNameOf<Package>() }

        target.instructions.insert(call, asm {
            pop
            ldc("org.spongepowered.asm.mixin")
        })

        target.instructions.remove(call)

        val writer = ClassWriter(reader, 0)
        node.accept(writer)
        return writer.toByteArray()
    }

    override fun findClass(name: String): Class<*> {
        if (name == javaClass.name) return javaClass

        return findLoadedClass(name) ?: if (shouldLoadParent(name)) parent.loadClass(name) else {
            val internalName = name.replace('.', '/')
            val bytes = getResourceAsStream("$internalName.class")?.readBytes()
                ?: throw ClassNotFoundException("Could not sandbox mixin: resource unavailable: $internalName")

            val resultBytes = transform(internalName, bytes) ?: bytes
            defineClass(name, resultBytes, 0, resultBytes.size)
        }
    }

    override fun loadClass(name: String): Class<*> = findClass(name)
    private fun shouldLoadParent(name: String) = name in loaderExclusions || systemClasses.any { name.startsWith(it) }

    /**
     * Allows you to add an "injected resource" to this loader, that is, when a resource with [name] is requested,
     * a stream (or temporary file) with [bytes] is returned (instead of delegating to the parent loader)
     */
    public fun injectResource(name: String, bytes: ByteArray) {
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

    /**
     * Allows you to prevent a certain class with a given binary [name] from being sandboxed
     */
    public fun addLoaderExclusion(name: String) {
        loaderExclusions += name
    }

    internal fun getClassBytes(name: String) = loader(name) ?: getResourceAsStream("$name.class")?.readBytes()
}

private fun createMixinAccessor(loader: ClassLoader) = runCatching {
    loader
        .loadClass("net.weavemc.loader.impl.mixin.MixinAccessImpl")
        .getField("INSTANCE").also { it.isAccessible = true }[null] as MixinAccess
}.onFailure {
    logger.error("Failed to create a mixin access instance", it)

    val dummy = object {}
    logger.debug(
        "Creating accessor within $loader, which is nested within ${loader.parent}, " +
                "while this method is called within ${dummy.javaClass.classLoader}, " +
                "and MixinAccess is accessed from within ${MixinAccess::class.java.classLoader}"
    )
}.getOrThrow()

/**
 * Keeps track of the state of the mixin environment. Allows you to interact with the sandbox.
 *
 * [initialize] must be invoked before calling any other method
 */
public class SandboxedMixinState(
    private val loader: SandboxedMixinLoader
) : MixinAccess by createMixinAccessor(loader) {
    // be careful to not load classes in the wrong context
    internal lateinit var transformer: Any

    /**
     * Whether this [SandboxedMixinState] has been initialized
     */
    public var initialized: Boolean = false
        private set

    /**
     * Initializes this [SandboxedMixinState], does nothing when [initialized] is true
     */
    public fun initialize() {
        if (initialized) return

        injectService(
            "org.spongepowered.asm.service.IMixinService",
            "net.weavemc.loader.impl.mixin.SandboxedMixinService"
        )

        injectService(
            "org.spongepowered.asm.service.IMixinServiceBootstrap",
            "net.weavemc.loader.impl.mixin.DummyServiceBootstrap"
        )

        injectService(
            "org.spongepowered.asm.service.IGlobalPropertyService",
            "net.weavemc.loader.impl.mixin.DummyPropertyService"
        )

        bootstrap()
        validate()
        gotoDefault()

        require(this::transformer.isInitialized) { "Why did I not get a transformer?" }
        initialized = true
    }

    /**
     * Registers a mixin configuration, available on the classpath of the parent loader with a given [resourceName]
     */
    public fun registerMixin(modId: String, resourceName: String) {
        val resource = loader.getResourceAsStream(resourceName) ?: error("Mixin config $resourceName does not exist!")

        // This is quite jank: we relocate the resources such that we can later find out what mod it was from
        val relocatedName = "weave-mod-mixin/$modId/${resourceName}"
        loader.injectResource(relocatedName, resource.use { it.readBytes() })

        addMixin(relocatedName)
    }

    /**
     * Transforms a class represented by given [bytes] with a certain [internalName] using the mixin environment
     */
    public fun transform(internalName: String, bytes: ByteArray): ByteArray =
        transform(transformer, internalName.replace('/', '.'), bytes)

    /**
     * Transforms a class represented by given [bytes] with a certain [internalName] using the mixin environment.
     *
     * Returns null when mixin was not interested in visiting this class
     */
    public fun transformOrNull(internalName: String, bytes: ByteArray): ByteArray? =
        transform(internalName, bytes).takeIf { it !== bytes }

    /**
     * Transforms a class represented by given [node] with a certain [internalName] using the mixin environment
     * Returns whether a transformation has been performed
     */
    public fun transform(internalName: String, node: ClassNode): Boolean =
        transform(transformer, internalName.replace('/', '.'), node)

    private fun injectService(name: String, value: String) =
        loader.injectResource("META-INF/services/$name", value.encodeToByteArray())
}

internal sealed interface MixinAccess {
    fun bootstrap()
    fun validate()
    fun gotoDefault()
    fun addMixin(name: String)
    fun transform(transformer: Any, internalName: String, bytes: ByteArray): ByteArray
    fun transform(transformer: Any, internalName: String, node: ClassNode): Boolean
    fun generateClass(transformer: Any, internalName: String): ByteArray?
    fun produceInjectedClasses(transformer: Any): Map<String, ByteArray>
    fun findTargets(transformer: Any): Set<String>
}

@Suppress("unused")
private data object MixinAccessImpl : MixinAccess {
    private val env get() = MixinEnvironment.getDefaultEnvironment()
    private var hasForcedSelect = false
    private var injectedClasses: Map<String, ByteArray>? = null

    override fun bootstrap() = MixinBootstrap.init()
    override fun validate() {
        val service = MixinService.getService()
        require(service is SandboxedMixinService) { "Invalid mixin service: $service" }
    }

    override fun gotoDefault() {
        // Usually does nothing, but the internal state kind of expects this to be the case
        MixinEnvironment.init(Phase.DEFAULT)
        MixinEnvironment::class.java.getDeclaredMethod("gotoPhase", Phase::class.java)
            .also { it.isAccessible = true }(null, Phase.DEFAULT)
    }

    override fun addMixin(name: String) = Mixins.addConfiguration(name)
    override fun transform(transformer: Any, internalName: String, bytes: ByteArray): ByteArray =
        (transformer as IMixinTransformer).transformClass(env, internalName, bytes)

    override fun transform(transformer: Any, internalName: String, node: ClassNode): Boolean =
        (transformer as IMixinTransformer).transformClass(env, internalName, node)

    override fun generateClass(transformer: Any, internalName: String): ByteArray? =
        (transformer as IMixinTransformer).generateClass(env, internalName)

    // TODO: properly append these to the game classloader (reflective defineClass?)
    override fun produceInjectedClasses(transformer: Any): Map<String, ByteArray> {
        injectedClasses?.let { return it }

        checkForcedSelect(transformer)
        val registry = (transformer as IMixinTransformer).extensions.syntheticClassRegistry
        val classes = registry.javaClass.getDeclaredField("classes")
            .also { it.isAccessible = true }[registry] as Map<*, *>

        return buildMap {
            classes.keys.forEach {
                if (it is String) generateClass(transformer, it)?.let { bytes -> put(it, bytes) }
            }
        }.also { injectedClasses = it }
    }

    private fun checkForcedSelect(transformer: Any) {
        if (hasForcedSelect) return

        val processor = retrieveProcessor(transformer)
        processor.javaClass.getDeclaredMethod("checkSelect", MixinEnvironment::class.java)
            .also { it.isAccessible = true }(processor, env)

        hasForcedSelect = true
    }

    private fun retrieveProcessor(transformer: Any) =
        transformer.javaClass.getDeclaredField("processor").also { it.isAccessible = true }[transformer]

    override fun findTargets(transformer: Any): Set<String> {
        checkForcedSelect(transformer)

        val processor = retrieveProcessor(transformer)
        val configs = (processor.javaClass.getDeclaredField("configs")
            .also { it.isAccessible = true }[processor] as List<*>)
            .filterIsInstance<IMixinConfig>()

        val mxnConfig = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig")
        val getClasses = mxnConfig.getMethod("getClasses").also { it.isAccessible = true }

        @Suppress("UNCHECKED_CAST")
        return configs.filter { it.name.startsWith("weave-mod-mixin/") }.flatMapTo(hashSetOf()) {
            it.targets + if (mxnConfig.isInstance(it)) getClasses(it) as List<String> else emptyList()
        }
    }
}

private fun <R> counter() = object : ReadOnlyProperty<R, Int> {
    private var underlying = 0
    override fun getValue(thisRef: R, property: KProperty<*>) = underlying++
}

/**
 * Utility that ensures that asm can find inheritance info when writing a class.
 * [useBytecodeInheritance] determines if inheritance info should be derived from bytecode instead of reflection, if possible
 */
// TODO: generalize
public class LoaderClassWriter(
    private val loader: ClassLoader?,
    reader: ClassReader? = null,
    flags: Int = 0,
    private val useBytecodeInheritance: Boolean = true,
) : ClassWriter(reader, flags) {
    private fun getStream(name: String) = loader?.getResourceAsStream(name)
        ?: LoaderClassWriter::class.java.getResourceAsStream(name)

    private fun String.load() =
        getStream("$this.class")?.readBytes()?.asClassReader()?.asClassNode()

    private val ClassNode.isInterface: Boolean
        get() = access and ACC_INTERFACE != 0

    override fun getCommonSuperClass(type1: String, type2: String): String {
        if (!useBytecodeInheritance) return super.getCommonSuperClass(type1, type2)
        return when {
            type1 == "java/lang/Object" || type2 == "java/lang/Object" -> "java/lang/Object"
            type1 == type2 -> type1
            else -> {
                val node1 = type1.load() ?: return super.getCommonSuperClass(type1, type2)
                val node2 = type2.load() ?: return super.getCommonSuperClass(type1, type2)

                when {
                    node1.isInterface || node2.isInterface -> "java/lang/Object"
                    else -> {
                        node1.getAllParents().intersect(node2.getAllParents().toSet())
                            .firstOrNull() ?: return super.getCommonSuperClass(type1, type2)
                    }
                }
            }
        }
    }

    private fun ClassNode.getAllParents() = listOf(name) + getParents()
    private fun ClassNode.getParents(): List<String> = when (name) {
        "java/lang/Object" -> emptyList()
        else -> listOf(superName) + (superName.load()?.getParents() ?: emptyList())
    }

    override fun getClassLoader(): ClassLoader? = loader
}