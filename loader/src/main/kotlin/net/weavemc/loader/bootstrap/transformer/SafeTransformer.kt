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
        val bytes = transform(loader, className, classfileBuffer)
        if (checkBytecode && bytes != null) {
            ClassReader(bytes).run {
                check(this.className == className) { "Class name mismatch: expected $className, got ${this.className}"}
            }
        }
        bytes
    }.getOrElse {
        it.printStackTrace()
        println("An error occurred while transforming $className (from ${this.javaClass.name}): ${it.message}")
        exitProcess(1)
    }
}