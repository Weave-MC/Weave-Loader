package net.weavemc.loader

import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.dump
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Paths

internal object HookManager : SafeTransformer {
    /**
     * JVM argument to dump bytecode to disk. Can be enabled by adding
     * `-DdumpBytecode=true` to your JVM arguments when launching with Weave.
     *
     * Defaults to `false`.
     */
    val dumpBytecode = System.getProperty("dumpBytecode")?.toBoolean() ?: false
    val hooks = mutableListOf<Hook>()

    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        val hooks = hooks.filter { it.targets.contains("*") || it.targets.contains(className) }
        if (hooks.isEmpty()) return null

        println("[HookManager] Transforming $className")
        hooks.forEach { println("  - ${it.javaClass.name}") }

        val node = ClassNode()
        val reader = ClassReader(originalClass)
        reader.accept(node, 0)

        val config = object : Hook.AssemblerConfig {
            var computeFrames = false
            override fun computeFrames() {
                computeFrames = true
            }
        }

        hooks.forEach { it.transform(node, config) }
        val flags = if (config.computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS

        val writer = HookClassWriter(reader, flags)
        node.accept(writer)

        if (dumpBytecode) {
            val bytecodeOut = getBytecodeDir().resolve("$className.class")
            runCatching {
                bytecodeOut.parentFile?.mkdirs()
                node.dump(bytecodeOut.absolutePath)
            }.onFailure { println("Failed to dump bytecode for $bytecodeOut") }
        }

        return writer.toByteArray()
    }

    /**
     * Returns
     *   - Windows: `%user.home%/.weave/.bytecode.out`
     *   - UN*X: `$HOME/.weave/.bytecode.out`
     */
    private fun getBytecodeDir() = Paths.get(System.getProperty("user.home"), ".weave", ".bytecode.out")
        .toFile().apply { mkdirs() }
}

class HookClassWriter(classReader: ClassReader, flags: Int) : ClassWriter(classReader, flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String =
        WeaveLoader.mixinSandbox.getCommonSuperClass(type1, type2)
}
