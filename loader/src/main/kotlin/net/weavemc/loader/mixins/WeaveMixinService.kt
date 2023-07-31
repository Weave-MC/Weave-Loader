package net.weavemc.loader.mixins

import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.gameClient
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
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
import java.io.IOException
import java.io.InputStream
import java.net.URL

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

    internal companion object {

        lateinit var transformer: IMixinTransformer
            private set
    }

    private val genesisClassCache by lazy {
        if (gameClient == GameInfo.Client.LUNAR)
            this.javaClass.classLoader.javaClass
                .declaredFields
                .find { Map::class.java.isAssignableFrom(it.type) }!!
                .also { it.isAccessible = true }
                .get(this.javaClass.classLoader)
                as Map<String, ByteArray>
        else
            emptyMap()
    }

    /**
     * @return the name of the service.
     */
    override fun getName(): String = "Weave"

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
            transformer = internal.createTransformer()
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
                ?: this.javaClass.classLoader.getResourceAsStream("$internalName.class")!!.readBytes()

            val cn = ClassNode()
            ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES)
            return cn
        } catch (ex: IOException) {
            throw ClassNotFoundException(canonicalName, ex)
        }
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
