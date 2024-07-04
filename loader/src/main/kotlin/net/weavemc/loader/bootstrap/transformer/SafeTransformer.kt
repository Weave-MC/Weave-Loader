package net.weavemc.loader.bootstrap.transformer

import org.objectweb.asm.ClassReader
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import kotlin.system.exitProcess

val checkBytecode = java.lang.Boolean.getBoolean("weave.loader.checkTransformedBytecode")

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
            .also { if (checkBytecode && it != null) verifyBytes(className, classfileBuffer, it) }
    }.getOrElse {
        it.printStackTrace()
        println("An error occurred while transforming $className (from ${javaClass.name}): ${it.message}")
        exitProcess(1)
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