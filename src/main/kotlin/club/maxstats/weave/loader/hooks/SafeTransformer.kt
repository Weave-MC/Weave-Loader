package club.maxstats.weave.loader.hooks

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import kotlin.system.exitProcess

abstract class SafeTransformer : ClassFileTransformer {
    abstract fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray?

    final override fun transform(
        loader: ClassLoader,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray? {
        try {
            return this.transform(loader, className, classfileBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    final override fun transform(
        module: Module?,
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray {
        return super.transform(module, loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
    }
}