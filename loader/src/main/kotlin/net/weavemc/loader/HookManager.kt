package net.weavemc.loader

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.dump
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.mapping.MappingsHandler
import net.weavemc.loader.mapping.MappingsHandler.classLoaderBytesProvider
import net.weavemc.loader.mapping.MappingsHandler.remap
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
        val matchedHooks = hooks.toList().filter { it.mappedTargets.contains(className) }
        if (matchedHooks.isEmpty()) return null

        println("[Weave] Hooking $className")

        val config = AssemblerConfigImpl()
        val classReader = ClassReader(originalClass)
        val classNode = applyHooks(classReader, matchedHooks, config)
        val classWriter = HookClassWriter(config.classWriterFlags, classReader)

        if (dumpBytecode) {
            val bytecodeOut = getBytecodeDir().resolve("$${className.replace("/", "_")}.class")
            runCatching {
                bytecodeOut.parentFile?.mkdirs()
                classNode.dump(bytecodeOut.absolutePath)
            }.onFailure { println("Failed to dump bytecode for $bytecodeOut") }
        }

        classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    private fun applyHooks(classReader: ClassReader, hooks: List<ModHook>, config: AssemblerConfigImpl): ClassNode {
        val classNode = classReader.asClassNode()

        // skip remapping, the environment namespace matches with the mods
        if (MappingsHandler.environmentNamespace == "official") {
            hooks.forEach { it.hook.transform(classNode, config) }
            return classNode
        }

        val vanillaClassNode = classNode.remap(MappingsHandler.environmentUnmapper, config.classWriterFlags)
        val environmentClassNode = vanillaClassNode.remap(MappingsHandler.environmentRemapper, config.classWriterFlags)

        return environmentClassNode
    }

    /**
     * Returns
     *   - Windows: `%user.home%/.weave/.bytecode.out`
     *   - UN*X: `$HOME/.weave/.bytecode.out`
     */
    private fun getBytecodeDir() = Paths.get(System.getProperty("user.home"), ".weave", ".bytecode.out")
        .toFile().apply { mkdirs() }

    private class AssemblerConfigImpl : Hook.AssemblerConfig {
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
    // Mods are always mapped to vanilla by Weave-Gradle
    val bytesProvider = classLoaderBytesProvider("named")
    private fun ClassNode.isInterface(): Boolean = (this.access and Opcodes.ACC_INTERFACE) != 0
    private fun ClassReader.isAssignableFrom(target: ClassReader): Boolean {
        val classes = ArrayDeque(listOf(target))

        while (classes.isNotEmpty()) {
            val cl = classes.removeFirst()
            if (cl.className == className) return true

            classes.addAll(
                (listOfNotNull(cl.superName) + cl.interfaces).map { ClassReader(bytesProvider(it)) }
            )
        }

        return false
    }

    override fun getCommonSuperClass(type1: String, type2: String): String {
        var class1 = bytesProvider(type1)?.asClassReader() ?: error("Failed to find type1 $type1")
        val class2 = bytesProvider(type2)?.asClassReader() ?: error("Failed to find type2 $type2")

        return when {
            class1.isAssignableFrom(class2) -> type1
            class2.isAssignableFrom(class1) -> type2
            class1.asClassNode().isInterface() || class2.asClassNode().isInterface() -> "java/lang/Object"
            else -> {
                while (!class1.isAssignableFrom(class2))
                    class1 = bytesProvider(class1.superName)!!.asClassReader()

                return class1.className
            }
        }
    }
}
