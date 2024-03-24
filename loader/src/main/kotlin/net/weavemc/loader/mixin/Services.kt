package net.weavemc.loader.mixin

import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.logging.LoggerAdapterConsole
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory
import org.spongepowered.asm.service.*
import org.spongepowered.asm.util.Constants
import org.spongepowered.asm.util.ReEntranceLock
import java.io.InputStream
import java.net.URL

internal object SandboxedClassProvider : IClassProvider, IClassBytecodeProvider {
    private val sandboxLoader get() = javaClass.classLoader as? SandboxedMixinLoader

    @Deprecated("Deprecated in Java", ReplaceWith("emptyArray()"))
    override fun getClassPath() = emptyArray<URL>()
    override fun findClass(name: String?) = findClass(name, false)
    override fun findAgentClass(name: String?, initialize: Boolean) = findClass(name, initialize)
    override fun findClass(name: String?, initialize: Boolean): Class<*> =
        Class.forName(name, initialize, javaClass.classLoader)

    override fun getClassNode(name: String) = getClassNode(name, false)
    override fun getClassNode(name: String, runTransformers: Boolean) =
        sandboxLoader?.getClassBytes(name.replace('.', '/'))?.asClassReader()?.asClassNode()
            ?: throw ClassNotFoundException(name.replace('/', '.'))
}

internal class SandboxedMixinService : IMixinService {
    private val logger = getLogger("Sandboxed Mixin")
    private val lock = ReEntranceLock(1)
    private val sandboxLoader get() = javaClass.classLoader as? SandboxedMixinLoader

    override fun getName() = "Sandboxed Mixin"
    override fun isValid() = true
    override fun prepare() {}
    override fun getInitialPhase(): MixinEnvironment.Phase = MixinEnvironment.Phase.PREINIT

    override fun offer(internal: IMixinInternal) {
        if (internal is IMixinTransformerFactory) {
            if (sandboxLoader == null) logger.warn("$sandboxLoader was null while transformer was constructed")
            sandboxLoader?.state?.transformer = internal.createTransformer()
        }
    }

    override fun init() {}
    override fun beginPhase() {}
    override fun checkEnv(bootSource: Any?) {}
    override fun getReEntranceLock() = lock
    override fun getClassProvider() = SandboxedClassProvider
    override fun getBytecodeProvider() = SandboxedClassProvider
    override fun getTransformerProvider() = null
    override fun getClassTracker() = null
    override fun getAuditTrail() = null
    override fun getPlatformAgents() = emptyList<String>()
    override fun getPrimaryContainer() = ContainerHandleVirtual(name)
    override fun getMixinContainers() = emptyList<IContainerHandle>()
    override fun getResourceAsStream(name: String): InputStream? = javaClass.classLoader.getResourceAsStream(name)
    override fun getSideName() = Constants.SIDE_CLIENT
    override fun getMinCompatibilityLevel() = null
    override fun getMaxCompatibilityLevel() = null
    override fun getLogger(name: String) = LoggerAdapterConsole(name)
}

@Suppress("unused")
internal class DummyServiceBootstrap : IMixinServiceBootstrap {
    override fun getName() = "Sandboxed Mixin Bootstrap"
    override fun getServiceClassName(): String = SandboxedMixinService::class.java.name
    override fun bootstrap() {}
}

@Suppress("unused")
internal class DummyPropertyService : IGlobalPropertyService {
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