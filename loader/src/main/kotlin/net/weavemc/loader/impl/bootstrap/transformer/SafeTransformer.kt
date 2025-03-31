package net.weavemc.loader.impl.bootstrap.transformer

import me.xtrm.klog.dsl.klog
import net.weavemc.loader.impl.util.exit
import org.objectweb.asm.ClassReader
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

private val checkBytecode = java.lang.Boolean.getBoolean("weave.loader.checkTransformedBytecode")
private val logger by klog

internal interface SafeTransformer : ClassFileTransformer {
    /**
     * @param loader The ClassLoader responsible for loading the class, can be null if the loader is the Bootstrap Loader
     * @param className The name of the class
     * @param originalClass The class file bytes
     */
    fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray?

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ) = runCatching {
        transform(loader, className, classfileBuffer)
            ?.also { if (checkBytecode) verifyBytes(className, classfileBuffer, it) }
    }.getOrElse {
        it.printStackTrace()
        logger.fatal("An error occurred while transforming {} (from {})", className, javaClass.name, it)
        exit(1)
    }

    private fun verifyBytes(className: String, original: ByteArray, transformed: ByteArray) {
        val reader = ClassReader(transformed)
        val transformedName = reader.className

        val originalVersion = (original[6].toInt() and 0xFF shl 8) or (original[7].toInt() and 0xFF)
        val transformedVersion = reader.readShort(6).toInt()

        check(transformedName == className) { "Class name mismatch: expected $className, got $transformedName" }
        check(transformedVersion <= originalVersion) {
            "Class version mismatch: expected $originalVersion or lower, got $transformedVersion"
        }
    }
}