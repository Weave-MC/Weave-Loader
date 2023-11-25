package net.weavemc.loader

import com.grappenmaker.mappings.MappingsRemapper
import net.weavemc.api.Hook
import net.weavemc.api.bytecode.dump
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.loader.mapping.MappingsHandler
import net.weavemc.loader.mapping.MappingsHandler.remap
import net.weavemc.loader.mapping.MappingsHandler.remapToEnvironment
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
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
        synchronized(hooks) {
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
    }

    private fun applyHooks(classReader: ClassReader, hooks: List<ModHook>): ClassNode {
        var previousNamespace = MappingsHandler.environmentNamespace
        var classNode = ClassNode().also { classReader.accept(it, 0) }
        var config = AssemblerConfigImpl()

        for (hook in hooks) {
            if (hook.mappings == previousNamespace) {
                hook.hook.transform(classNode, config)
            } else {
                val remapper = MappingsRemapper(
                    MappingsHandler.fullMappings,
                    previousNamespace,
                    hook.mappings,
                    loader = MappingsHandler.jarBytesProvider(listOf(), previousNamespace)
                )
                val newClassNode = classNode.remap(remapper, config.classWriterFlags)

                // filter methods between old and new class nodes that have the same name and desc
                for ((index, newMethod) in newClassNode.methods.withIndex()) {
                    var revert = false

                    fun checkDuplicate() {
                        // find a method in the new class node that is before newMethod and has the same name and desc
                        val filteredMethod = newClassNode
                            .methods
                            .find { it.name == newMethod.name && it.desc == newMethod.desc }
                            ?: return
                        if (filteredMethod == newMethod) {
                            return
                        }

                        revert = true
                    }

                    fun checkSpongePoweredMixinAnnotation() {
                        if (newMethod.visibleAnnotations?.any { it.desc.startsWith("Lorg/spongepowered/asm/mixin/") } == true) {
                            revert = true
                        }
                    }

                    checkDuplicate()
                    checkSpongePoweredMixinAnnotation()

                    if (revert) {
                        // revert the name of the method to the old name
                        val oldMethod = classNode.methods[index]
                        newMethod.name = oldMethod.name
                    }
                }

                // filter methods that were mapped and add an annotation to them
                for ((index, oldMethod) in classNode.methods.withIndex()) {
                    val newMethod = newClassNode.methods[index]

                    if (oldMethod.name != newMethod.name || oldMethod.desc != newMethod.desc) {
                        if (newMethod.invisibleAnnotations == null) {
                            newMethod.invisibleAnnotations = mutableListOf()
                        }

                        if (!newMethod.invisibleAnnotations.any { it.desc == "L${internalNameOf<Mapped>()};" }) {
                            newMethod.invisibleAnnotations.add(AnnotationNode("L${internalNameOf<Mapped>()};"))
                        }
                    }
                }

                classNode = newClassNode

                config = AssemblerConfigImpl()
                hook.hook.transform(classNode, config)

                previousNamespace = hook.mappings
            }
        }

        // remap back to the environment
        if (previousNamespace != MappingsHandler.environmentNamespace) {
            val remapper = MappingsRemapper(
                MappingsHandler.fullMappings,
                previousNamespace,
                MappingsHandler.environmentNamespace,
                loader = MappingsHandler.jarBytesProvider(listOf(), previousNamespace)
            )

            // 20 a-Z
            val randomString = (1..20).map { ('Z'..'a').random() }.joinToString("")

            // add a random string to the name of the method ($methodName_$randomString)
            classNode
                .methods
                .filter { method -> method.invisibleAnnotations?.any { it.desc == "L${internalNameOf<Mapped>()};" } != true }
                .forEach { method -> method.name = "${method.name}_$randomString" }

            val newClassNode = classNode.remap(remapper, config.classWriterFlags)

            // revert the name of the method to the old name
            newClassNode
                .methods
                .filter { method -> method.invisibleAnnotations?.any { it.desc == "L${internalNameOf<Mapped>()};" } != true }
                .forEach { method -> method.name = method.name.removeSuffix("_$randomString") }

            // remove the Mapped annotation
            newClassNode
                .methods
                .forEach { method -> method.invisibleAnnotations?.removeIf { it.desc == "L${internalNameOf<Mapped>()};" } }

            classNode = newClassNode
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

    private class AssemblerConfigImpl : Hook.AssemblerConfig {
        var computeFrames = false

        override fun computeFrames() {
            computeFrames = true
        }

        val classWriterFlags: Int
            get() = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS
    }

    private annotation class Mapped
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
