package net.weavemc.loader.bootstrap.transformer

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import kotlin.system.exitProcess

internal interface SafeTransformer : ClassFileTransformer {
    fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray?

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ) = try {
        protectionDomain?.codeSource?.codeSigners
        if (loader != null) transform(loader, className, classfileBuffer) else null
    } catch (e: Throwable) {
        e.printStackTrace()
        exitProcess(1)
    }
}
