package net.weavemc.loader

import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.loader.mapping.*
import net.weavemc.api.Hook
import net.weavemc.api.bytecode.dump
import net.weavemc.loader.mapping.MappingsHandler.remapToEnvironment
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
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
    val hooks = mutableListOf<ModHook>()

    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        // TODO: this looks like it will not work: there is no filtering
        val hooks = this.hooks.map { it.hook }
        if (hooks.isEmpty()) return null

        println("[HookManager] Transforming $className")

        val node = ClassNode()
        val reader = ClassReader(originalClass)
        reader.accept(node, 0)

        val config = object : Hook.AssemblerConfig {
            var computeFrames = false
            override fun computeFrames() {
                computeFrames = true
            }
        }

        val flags = if (config.computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS
        val writer = HookClassWriter(flags, reader)

        // TODO: reimplement hooks

        if (dumpBytecode) {
            val bytecodeOut = getBytecodeDir().resolve("$className.class")
            runCatching {
                bytecodeOut.parentFile?.mkdirs()
                node.dump(bytecodeOut.absolutePath)
            }.onFailure { println("Failed to dump bytecode for $bytecodeOut") }
        }

        node.accept(writer)
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

class HookClassWriter(
    flags: Int,
    reader: ClassReader? = null,
) : ClassWriter(reader, flags) {
    private fun getResourceAsByteArray(resourceName: String): ByteArray =
        classLoader.getResourceAsStream("$resourceName.class")?.readBytes()
            ?: classLoader.getResourceAsStream("${MappingsHandler.resourceNameMapper.map(resourceName)}.class")
                ?.readBytes()?.remapToEnvironment()
            ?: throw ClassNotFoundException("Failed to retrieve class from resources: $resourceName")

    private fun ByteArray.asClassReader(): ClassReader = ClassReader(this)
    private fun ClassReader.asClassNode(): ClassNode {
        val cn = ClassNode()
        accept(cn, 0)
        return cn
    }

    private fun ClassNode.isInterface(): Boolean = (this.access and Opcodes.ACC_INTERFACE) != 0
    private fun ClassReader.isAssignableFrom(target: ClassReader): Boolean {
        val classes = ArrayDeque(listOf(target))

        while (classes.isNotEmpty()) {
            val cl = classes.removeFirst()
            if (cl.className == className) return true

            classes.addAll(
                (listOfNotNull(cl.superName) + cl.interfaces).map { ClassReader(getResourceAsByteArray(it)) }
            )
        }

        return false
    }

    override fun getCommonSuperClass(type1: String, type2: String): String {
        var class1 = getResourceAsByteArray(type1).asClassReader()
        val class2 = getResourceAsByteArray(type2).asClassReader()

        return when {
            class1.isAssignableFrom(class2) -> type1
            class2.isAssignableFrom(class1) -> type2
            class1.asClassNode().isInterface() || class2.asClassNode().isInterface() -> "java/lang/Object"
            else -> {
                while (!class1.isAssignableFrom(class2))
                    class1 = getResourceAsByteArray(class1.superName).asClassReader()

                return class1.className
            }
        }
    }
}
