package net.weavemc.loader

import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.dump
import net.weavemc.weave.api.gameVersion
import net.weavemc.weave.api.mapping.XSrgRemapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
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

        var computeFrames = false
        val cfg = Hook.AssemblerConfig { computeFrames = true }

        hooks.forEach { it.transform(node, cfg) }
        val flags = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_FRAMES

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
    internal fun getBytecodeDir(): File {
       return Paths.get(System.getProperty("user.home"), ".weave", ".bytecode.out").toFile().apply { mkdirs() }
    }
}

class HookClassWriter(classReader: ClassReader, flags: Int): ClassWriter(classReader, flags) {
    val vanillaRemapper = XSrgRemapper(gameVersion, "notch")
    private fun getResourceAsByteArray(resourceName: String): ByteArray =
        classLoader.getResourceAsStream("$resourceName.class")?.readBytes()
            ?: classLoader.getResourceAsStream("${vanillaRemapper.mapClass(resourceName)}.class")?.readBytes()
            ?: throw ClassNotFoundException("Failed to retrieve class from resources: $resourceName")
    private fun ByteArray.asClassReader(): ClassReader = ClassReader(this)
    private fun ClassReader.asClassNode(): ClassNode {
        val cn = ClassNode()
        this.accept(cn, 0)
        return cn
    }
    private fun ClassNode.isInterface(): Boolean = (this.access and Opcodes.ACC_INTERFACE) != 0
    private fun ClassReader.isAssignableFrom(target: ClassReader): Boolean {
        val classes = ArrayDeque(listOf(target))

        while (classes.isNotEmpty()) {
            val cl = classes.removeFirst()
            if (cl.className == this.className) return true

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

                return vanillaRemapper.reverseMapClass(class1.className) ?: class1.className
            }
        }
    }
}
