package club.maxstats.weave.loader.mixins.service

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

public class WeaveMixinService : IMixinService, IClassProvider, IClassBytecodeProvider {

    private val lock = ReEntranceLock(1)

    internal companion object {

        lateinit var transformer: IMixinTransformer
            private set
    }

    private val genesisClassCache by lazy {
        this.javaClass.classLoader.javaClass
            .declaredFields
            .find { Map::class.java.isAssignableFrom(it.type) }!!
            .also { it.isAccessible = true }
            .get(this.javaClass.classLoader)
            as Map<String, ByteArray>
    }

    override fun getName(): String = "Weave"

    override fun isValid(): Boolean = true

    override fun prepare() {}

    override fun getInitialPhase(): MixinEnvironment.Phase = MixinEnvironment.Phase.DEFAULT

    override fun offer(internal: IMixinInternal) {
        if (internal is IMixinTransformerFactory) {
            transformer = internal.createTransformer()
        }
    }

    override fun init() {}

    override fun beginPhase() {}

    override fun checkEnv(bootSource: Any?) {}

    override fun getReEntranceLock(): ReEntranceLock = lock

    override fun getClassProvider(): IClassProvider = this

    override fun getBytecodeProvider(): IClassBytecodeProvider = this

    override fun getTransformerProvider(): ITransformerProvider? = null

    override fun getClassTracker(): IClassTracker? = null

    override fun getAuditTrail(): IMixinAuditTrail? = null

    override fun getPlatformAgents(): Collection<String> = emptyList()

    override fun getPrimaryContainer(): IContainerHandle = ContainerHandleVirtual(name)

    override fun getMixinContainers(): Collection<IContainerHandle> = emptyList()

    override fun getResourceAsStream(name: String?): InputStream? =
        ClassLoader.getSystemResourceAsStream(name)

    override fun getSideName(): String = Constants.SIDE_CLIENT

    override fun getMinCompatibilityLevel(): MixinEnvironment.CompatibilityLevel? = null

    override fun getMaxCompatibilityLevel(): MixinEnvironment.CompatibilityLevel? = null

    override fun getLogger(name: String?): ILogger = LoggerAdapterConsole(name)

    /*
    * IClassProvider
    * */

    override fun getClassPath(): Array<URL> = emptyArray()

    override fun findClass(name: String?): Class<*> = Class.forName(name)

    override fun findClass(name: String?, initialize: Boolean): Class<*> =
        Class.forName(name, initialize, this.javaClass.classLoader)

    override fun findAgentClass(name: String?, initialize: Boolean): Class<*> =
        findClass(name, initialize)

    /*
    * IClassBytecodeProvider
    * */

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

    override fun getClassNode(name: String, runTransformers: Boolean): ClassNode =
        getClassNode(name)
}
