package net.weavemc.loader.impl

import com.grappenmaker.mappings.ClasspathLoader
import com.grappenmaker.mappings.ClasspathLoaders
import com.grappenmaker.mappings.remap.LambdaAwareRemapper
import com.grappenmaker.mappings.remap.remap
import me.xtrm.klog.dsl.klog
import net.weavemc.api.Hook
import net.weavemc.internals.dump
import net.weavemc.loader.impl.bootstrap.Bootstrap
import net.weavemc.loader.impl.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.impl.util.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import kotlin.io.path.createDirectories
import kotlin.random.Random
import kotlin.random.nextUInt

public object InjectionHandler : SafeTransformer {
    private val logger by klog

    /**
     * JVM argument to dump bytecode to disk. Can be enabled by adding
     * `-Dweave.dump.bytecode.enabled=true` to your JVM arguments when launching with Weave.
     *
     * Defaults to `false`.
     */
    public val dumpBytecode: Boolean by systemProperty("weave.dump.bytecode.enabled", false)

    private val modifiers = mutableListOf<Modifier>()

    public fun registerModifier(modifier: Modifier) {
        logger.debug("Registering modifier for namespace '${modifier.namespace}' targeting ${modifier.targets.size} classes")
        modifiers += modifier
    }

    private fun ClassNode.remap(from: String, to: String) {
        if (from != to) {
            val oldName = name
            remap(MappingsHandler.mapper(from, to))
            logger.trace("Remapped class $oldName -> $name (from $from to $to)")
        }
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        originalClass: ByteArray
    ): ByteArray? {
        val groupedModifiers = modifiers.filter { className in it.targets }.groupBy { it.namespace }
        if (groupedModifiers.isEmpty()) return null

        logger.debug("Transforming $className with ${groupedModifiers.values.sumOf { it.size }} modifiers across ${groupedModifiers.size} namespaces")

        with(MappingsHandler) {
            val classReader = originalClass.asClassReader()
            val node = classReader.asClassNode()

            // Hack: temporarily preserve already @MixinMerged methods, such that collisions will never occur
            val potentialConflicts = node.methods.filter { it.hasMixinAnnotation("MixinMerged") }
            val conflictsMapping = hashMapOf<String, String>()
            val inverseConflictsMapping = hashMapOf<String, String>()

            for (m in potentialConflicts) {
                val tempName = "potentialConflict${Random.nextUInt()}"
                conflictsMapping["${node.name}.${m.name}${m.desc}"] = tempName
                inverseConflictsMapping["${node.name}.${tempName}${m.desc}"] = m.name
            }

            if (conflictsMapping.isNotEmpty()) {
                logger.trace("Preserving ${conflictsMapping.size} potential MixinMerged conflicts in $className")
            }

            // Hack: SimpleRemapper.map() can return null, and that breaks remap()
            node.remap(object : SimpleRemapper(Opcodes.ASM9, conflictsMapping) {
                override fun map(key: String): String {
                    return super.map(key) ?: key.run {
                        // for an unknown reason, `key` isn't just internal name only
                        // for example, <owner>.<name><descriptor> is sometimes passed to this method

                        if (contains('.')) {
                            if (contains('(')) substringAfter('.').substringBefore('(')
                            else substringAfter('.')
                        } else this
                    }
                }
            })

            val hookConfig = AssemblerConfigImpl()
            val modNs = groupedModifiers.keys

            // first FROM env namespace to all other namespaces to apply modifiers,
            // then finally remap to env namespace and apply its modifiers, instantly done.
            val nsOrder = buildList {
                add(environmentRuntimeNamespace)
                addAll(modNs - environmentRuntimeNamespace)
                add(environmentRuntimeNamespace)
            }

            for (i in 0..<nsOrder.lastIndex) {
                val last = nsOrder[i]
                val curr = nsOrder[i + 1]

                node.remap(last, curr)
                groupedModifiers[curr]?.forEach { modifier ->
                    logger.trace("Applying modifier (${modifier.javaClass.simpleName}) on $className in namespace $curr")
                    modifier.apply(node, hookConfig)
                }
            }

            val classWriter = InjectionClassWriter(hookConfig.classWriterFlags, classReader)
            node.accept(LambdaAwareRemapper(
                classWriter,
                SimpleRemapper(Opcodes.ASM9, inverseConflictsMapping)
            ))

            val transformedBytes = classWriter.toByteArray()

            if (dumpBytecode) {
                val bytecodeOut = FileManager.DUMP_DIRECTORY.resolve("$className.class")
                    .also { it.parent.createDirectories() }.toFile()

                runCatching {
                    logger.trace("Dumping transformed bytecode for $className to $bytecodeOut")
                    transformedBytes.dump(bytecodeOut.absolutePath)
                }.onFailure { logger.error("Failed to dump bytecode for $bytecodeOut", it) }
            }

            logger.debug("Successfully transformed $className")
            return transformedBytes
        }
    }
}

public interface Modifier {
    public val namespace: String
    public val targets: Set<String>
    public fun apply(node: ClassNode, cfg: Hook.AssemblerConfig)
}

/**
 * @param hook Hook class
 */
public data class ModHook(
    override val namespace: String,
    val hook: Hook,
    // TODO: jank
    override val targets: Set<String> = hook.targets.mapTo(hashSetOf()) {
        MappingsHandler.mapper(namespace, MappingsHandler.environmentRuntimeNamespace).map(it)
    }
): Modifier {
    override fun apply(node: ClassNode, cfg: Hook.AssemblerConfig): Unit = hook.transform(node, cfg)
}

public class AssemblerConfigImpl : Hook.AssemblerConfig {
    public var computeFrames: Boolean = false

    override fun computeFrames() {
        computeFrames = true
    }

    public val classWriterFlags: Int
        get() = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS
}

public class InjectionClassWriter(
    flags: Int,
    reader: ClassReader? = null,
) : ClassWriter(reader, flags) {
    public val bytesProvider: ClasspathLoader = ClasspathLoaders.compound(
        MappingsHandler.classLoaderBytesProvider(MappingsHandler.environmentRuntimeNamespace),
        ClasspathLoaders.fromLoader(Bootstrap.minecraftBootstrapClassLoader ?: error("Somehow minecraftBootstrapClassLoader is null"))
    )

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
        fun getClassReader(type: String): ClassReader =
            bytesProvider(type)?.asClassReader() ?: error("Failed to find class bytes for type: $type")

        var class1 = getClassReader(type1)
        val class2 = getClassReader(type2)

        return when {
            class1.isAssignableFrom(class2) -> type1
            class2.isAssignableFrom(class1) -> type2
            class1.asClassNode().isInterface() || class2.asClassNode().isInterface() -> "java/lang/Object"
            else -> {
                while (!class1.isAssignableFrom(class2)) {
                    val superName = class1.superName
                    class1 = bytesProvider(superName)?.asClassReader()
                        ?: error("Failed to load superclass $superName while computing common superclass for $type1 and $type2")
                }

                class1.className
            }
        }
    }
}