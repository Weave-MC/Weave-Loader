package net.weavemc.loader

import com.grappenmaker.mappings.asASMMapping
import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.loader.mapping.*
import net.weavemc.api.Hook
import net.weavemc.api.bytecode.dump
import net.weavemc.loader.mapping.MappingsHandler.remap
import net.weavemc.loader.mapping.MappingsHandler.remapToEnvironment
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
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
        val matchedHooks = hooks.filter { it.mappedTarget.contains(className) }
        if (matchedHooks.isEmpty()) {
            return null
        }

        println("[HookManager] Transforming $className")

        val classReader = ClassReader(originalClass)
        val classNode = applyHooks(classReader, matchedHooks)
        val classWriter = HookClassWriter(ClassWriter.COMPUTE_FRAMES, classReader)

        if (dumpBytecode) {
            val bytecodeOut = getBytecodeDir().resolve("$className.class")
            runCatching {
                bytecodeOut.parentFile?.mkdirs()
                classNode.dump(bytecodeOut.absolutePath)
            }.onFailure { println("Failed to dump bytecode for $bytecodeOut") }
        }

        classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    fun applyHooks(classReader: ClassReader, hooks: List<ModHook>): ClassNode {
        var previousNamespace = MappingsHandler.environmentNamespace
        var classNode = ClassNode().also { classReader.accept(it, 0) }
        var config = AssemblerConfigImpl()

        for (hook in hooks) {
            if (hook.mappings == previousNamespace) {
                hook.hook.transform(classNode, config)
            } else {
                val remapper = MappingsHandler.mapper(previousNamespace, hook.mappings)
                classNode = classNode.remap(remapper, config.classWriterFlags)
                config = AssemblerConfigImpl()
                hook.hook.transform(classNode, config)
                previousNamespace = hook.mappings
            }
        }

        // remap back to the environment
        if (previousNamespace != MappingsHandler.environmentNamespace) {
            val remapper = MappingsHandler.cachedUnmapper(previousNamespace)
            classNode = classNode.remap(remapper, config.classWriterFlags)
        }

        return classNode
    }

    /**
     * Returns
     *   - Windows: `%user.home%/.weave/.bytecode.out`
     *   - UN*X: `$HOME/.weave/.bytecode.out`
     */
    private fun getBytecodeDir() = Paths.get(System.getProperty("user.home"), ".weave", ".bytecode.out")
        .toFile().apply { mkdirs() }

    internal class AssemblerConfigImpl : Hook.AssemblerConfig {
        var computeFrames = false

        override fun computeFrames() {
            computeFrames = true
        }

        val classWriterFlags: Int
            get() = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS
    }
}

class HookClassWriter(
    flags: Int,
    reader: ClassReader? = null,
) : ClassWriter(reader, flags) {
    private fun getResourceAsByteArray(resourceName: String): ByteArray =
        classLoader.getResourceAsStream("${resourceName.also { println("resource name: $it") }}.class")?.readBytes()
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
