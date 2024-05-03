package net.weavemc.loader.mixin

import me.xtrm.klog.dsl.klog
import net.weavemc.loader.WeaveLogAppender
import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import net.weavemc.loader.util.getJavaVersion
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual
import org.spongepowered.asm.launch.platform.container.IContainerHandle
import org.spongepowered.asm.logging.ILogger
import org.spongepowered.asm.logging.Level
import org.spongepowered.asm.logging.LoggerAdapterAbstract
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
    private val logger by klog

    private val lock = ReEntranceLock(1)
    private val sandboxLoader get() = javaClass.classLoader as? SandboxedMixinLoader

    override fun getName() = "weave-mixin (within ${sandboxLoader?.parent})"
    override fun isValid() = true
    override fun prepare() {}
    override fun getInitialPhase(): MixinEnvironment.Phase = MixinEnvironment.Phase.PREINIT

    override fun offer(internal: IMixinInternal) {
        if (internal is IMixinTransformerFactory) {
            if (sandboxLoader == null)
                logger.warn("\"sandboxLoader\" was null while transformer was constructed")
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
    override fun getMinCompatibilityLevel() = MixinEnvironment.CompatibilityLevel.JAVA_7
    override fun getMaxCompatibilityLevel() = runCatching {
        MixinEnvironment.CompatibilityLevel.valueOf("JAVA_${getJavaVersion()}")
    }.getOrNull()
    override fun getLogger(name: String) = WeaveLoggerAdapter(name)
}

@Suppress("unused")
internal class DummyServiceBootstrap : IMixinServiceBootstrap {
    override fun getName() = "weave-mixin Bootstrap"
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

private typealias KlogLevel = me.xtrm.klog.Level
private class WeaveLoggerAdapter(name: String) : LoggerAdapterAbstract(name) {
    private val logger by klog(name)
    private val stdout = WeaveLogAppender.WrappingStream(System.out)

    override fun getType(): String {
        return "Default Console Logger"
    }

    override fun catching(level: Level, t: Throwable) {
        logger.warn("Catching {}: {}", t.javaClass.name, t.message, t)
    }

    override fun log(level: Level, message: String, vararg params: Any) {
        val formatted = FormattedMessage(message, *params)
        logger.log(level.toKlog(), formatted.message)
        if (formatted.hasThrowable()) {
            formatted.throwable.printStackTrace(stdout)
        }
    }

    override fun log(level: Level, message: String, t: Throwable) {
        logger.log(level.toKlog(), message)
        t.printStackTrace(stdout)
    }

    override fun <T : Throwable> throwing(throwable: T?): T? {
        this.log(
            Level.WARN, "Throwing {}: {}",
            throwable?.javaClass?.getName() ?: "null",
            throwable?.message ?: "null",
            throwable ?: Throwable("No Throwable")
        )
        return throwable
    }

    private fun Level.toKlog(): KlogLevel = when (this) {
        Level.TRACE -> KlogLevel.TRACE
        Level.DEBUG -> KlogLevel.DEBUG
        Level.INFO -> KlogLevel.INFO
        Level.WARN -> KlogLevel.WARN
        Level.ERROR -> KlogLevel.ERROR
        Level.FATAL -> KlogLevel.FATAL
    }
}
