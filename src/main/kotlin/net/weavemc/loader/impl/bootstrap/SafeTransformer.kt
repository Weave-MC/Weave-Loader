package net.weavemc.loader.impl.bootstrap

import me.xtrm.klog.dsl.klog
import net.weavemc.loader.impl.util.exit
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

private val checkBytecode = java.lang.Boolean.getBoolean("weave.loader.checkTransformedBytecode")
private val dumpBytecode = java.lang.Boolean.getBoolean("weave.loader.dumpTransformedBytecode")

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
        if (dumpBytecode && bytes != null) {
            klog.trace("Dumping transformed bytecode for {}", className)
            Path(".weave-transformed/$className.class").also {
                it.parent.createDirectories()
            }.writeBytes(bytes)
        }
        if (checkBytecode && bytes != null) {
            ClassNode().also {
                ClassReader(bytes).accept(it, 0)
            }.apply {
                check(this.name == className) { "Class name mismatch: expected $className, got ${this.name}" }
                check(this.version <= 52) { "Class version mismatch: expected 52 or lower, got ${this.version}" }
            }
        }
        bytes
    }.getOrElse {
        klog.fatal("An error occurred while transforming {} (from {})", className, this.javaClass.name, it)
        exit(1)
    }
}