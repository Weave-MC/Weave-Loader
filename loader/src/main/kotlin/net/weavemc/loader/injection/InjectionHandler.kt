package net.weavemc.loader.injection

import net.weavemc.api.Hook
import net.weavemc.internals.dump
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.mapping.MappingsHandler
import net.weavemc.loader.mapping.MappingsHandler.remap
import net.weavemc.loader.util.FileManager
import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import net.weavemc.loader.util.pushToFirst
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

object InjectionHandler : SafeTransformer {
    /**
     * JVM argument to dump bytecode to disk. Can be enabled by adding
     * `-DdumpBytecode=true` to your JVM arguments when launching with Weave.
     *
     * Defaults to `false`.
     */
    val dumpBytecode = System.getProperty("dumpBytecode")?.toBoolean() ?: true

    private val modifiers = mutableListOf<Modifier>()

    fun registerModifier(modifier: Modifier) {
        modifiers += modifier
    }

    private fun ClassNode.remap(from: String, to: String) =
        if (from == to) this else this.remap(MappingsHandler.mapper(from, to))

    override fun transform(
        loader: ClassLoader,
        className: String,
        originalClass: ByteArray
    ): ByteArray? {
        val groupedModifiers = modifiers.filter { className in it.targets }.groupBy { it.namespace }
        if (groupedModifiers.isEmpty()) return null

        with(MappingsHandler) {
            val classReader = originalClass.asClassReader()
            val classNode = classReader.asClassNode()

            val hookConfig = AssemblerConfigImpl()

            val modifierNamespaces = groupedModifiers.keys.pushToFirst(environmentNamespace)
            val start = classNode to environmentNamespace

            val (finalNode, lastNamespace) = modifierNamespaces.fold(start) { (modifiedNode, lastNamespace), currentNamespace ->
                val mappedNode = modifiedNode.remap(lastNamespace, currentNamespace)
                groupedModifiers.getValue(currentNamespace).forEach { it.apply(mappedNode) }
                mappedNode to currentNamespace
            }

            val classWriter = InjectionClassWriter(hookConfig.classWriterFlags, classReader)
            finalNode.remap(lastNamespace, environmentNamespace).accept(classWriter)

            if (dumpBytecode) {
                val bytecodeOut = FileManager.DUMP_DIRECTORY.resolve("$className.class").toFile()
                runCatching {
                    classWriter.toByteArray().dump(bytecodeOut.absolutePath)
                }.onFailure { println("Failed to dump bytecode for $bytecodeOut") }
            }

            return classWriter.toByteArray()
        }
    }
}

interface Modifier {
    val namespace: String
    val targets: List<String>
    fun apply(node: ClassNode)
}
/**
 * @param hook Hook class
 */
data class ModHook(
    override val namespace: String,
    val hook: Hook,
    override val targets: List<String> = hook.targets.map { MappingsHandler.environmentRemapper.map(it) }
): Modifier {
    override fun apply(node: ClassNode) = hook.transform(node, TODO())
}

//data class ModMixin(
//    override val namespace: String,
//    val mixin: Mixin,
//    val target: String = MappingsHandler.environmentRemapper.map(mixin.target)
//): Modifier {
//    override val targets = listOf(target)
//    override fun apply(node: ClassNode) = mixin.transform(node)
//}
//
//interface Mixin {
//    fun transform(classNode: ClassNode)
//    val target: String
//}

class AssemblerConfigImpl : Hook.AssemblerConfig {
    var computeFrames = false

    override fun computeFrames() {
        computeFrames = true
    }

    val classWriterFlags: Int
        get() = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS
}

class InjectionClassWriter(
    flags: Int,
    reader: ClassReader? = null,
) : ClassWriter(reader, flags) {
    // Mods are always mapped to vanilla by Weave-Gradle
    val bytesProvider = MappingsHandler.classLoaderBytesProvider(MappingsHandler.environmentNamespace)
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