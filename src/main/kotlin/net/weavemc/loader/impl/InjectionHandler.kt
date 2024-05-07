package net.weavemc.loader.impl

import com.grappenmaker.mappings.LambdaAwareRemapper
import me.xtrm.klog.dsl.klog
import net.weavemc.api.Hook
import net.weavemc.internals.dump
import net.weavemc.loader.impl.bootstrap.SafeTransformer
import net.weavemc.loader.impl.util.*
import net.weavemc.loader.impl.util.MappingsHandler.remap
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import kotlin.io.path.createDirectories
import kotlin.random.Random
import kotlin.random.nextUInt

object InjectionHandler : SafeTransformer {
    /**
     * JVM argument to dump bytecode to disk. Can be enabled by adding
     * `-DdumpBytecode=true` to your JVM arguments when launching with Weave.
     *
     * Defaults to `false`.
     */
    val dumpBytecode = System.getProperty("dumpBytecode")?.toBoolean() ?: false

    private val modifiers = mutableListOf<Modifier>()

    fun registerModifier(modifier: Modifier) {
        modifiers += modifier
    }

    private fun ClassNode.remap(from: String, to: String) =
        if (from == to) this else this.remap(MappingsHandler.mapper(from, to))

    override fun transform(
        loader: ClassLoader?,
        className: String,
        originalClass: ByteArray
    ): ByteArray? {
        val groupedModifiers = modifiers.filter { className in it.targets }.groupBy { it.namespace }
        if (groupedModifiers.isEmpty()) return null

        with(MappingsHandler) {
            val classReader = originalClass.asClassReader()
            val originalNode = classReader.asClassNode()

            // Hack: temporarily preserve already @MixinMerged methods, such that collisions will never occur
            val potentialConflicts = originalNode.methods.filter { it.hasMixinAnnotation("MixinMerged") }
            val conflictsMapping = hashMapOf<String, String>()
            val inverseConflictsMapping = hashMapOf<String, String>()

            for (m in potentialConflicts) {
                val tempName = "potentialConflict${Random.nextUInt()}"
                conflictsMapping["${originalNode.name}.${m.name}${m.desc}"] = tempName
                inverseConflictsMapping["${originalNode.name}.${tempName}${m.desc}"] = m.name
            }

            val classNode = ClassNode()
            originalNode.accept(LambdaAwareRemapper(classNode, SimpleRemapper(conflictsMapping)))

            val hookConfig = AssemblerConfigImpl()

            val modifierNamespaces = groupedModifiers.keys.pushToFirst(environmentNamespace)
            val start = classNode to environmentNamespace

            val (finalNode, lastNamespace) = modifierNamespaces.fold(start) { (modifiedNode, lastNamespace), currentNamespace ->
                val mappedNode = modifiedNode.remap(lastNamespace, currentNamespace)
                val nextNode = groupedModifiers.getValue(currentNamespace)
                    .fold(mappedNode) { acc, curr -> curr.apply(acc, hookConfig) }

                nextNode to currentNamespace
            }

            val classWriter = InjectionClassWriter(hookConfig.classWriterFlags, classReader)
            finalNode.remap(lastNamespace, environmentNamespace)
                .accept(LambdaAwareRemapper(classWriter, SimpleRemapper(inverseConflictsMapping)))

            if (dumpBytecode) {
                val bytecodeOut = FileManager.DUMP_DIRECTORY.resolve("$className.class")
                    .also { it.parent.createDirectories() }.toFile()

                runCatching {
                    classWriter.toByteArray().dump(bytecodeOut.absolutePath)
                }.onFailure { klog.error("Failed to dump bytecode for $bytecodeOut", it) }
            }

            return classWriter.toByteArray()
        }
    }
}

interface Modifier {
    val namespace: String
    val targets: Set<String>
    fun apply(node: ClassNode, cfg: Hook.AssemblerConfig): ClassNode
}

/**
 * @param hook Hook class
 */
data class ModHook(
    override val namespace: String,
    val hook: Hook,
    // TODO: jank
    override val targets: Set<String> = hook.targets.mapTo(hashSetOf()) {
        MappingsHandler.mapper(namespace, MappingsHandler.environmentNamespace).map(it)
    }
) : Modifier {
    override fun apply(node: ClassNode, cfg: Hook.AssemblerConfig): ClassNode {
        hook.transform(node, cfg)
        return node
    }
}

class AssemblerConfigImpl : Hook.AssemblerConfig {
    private var computeFrames = false

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