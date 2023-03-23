package club.maxstats.weave.loader.bootstrap

import club.maxstats.weave.loader.api.mixin.Inject
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Objects
import java.util.jar.JarFile

public class MixinApplicator {
    private val mixins = mutableListOf<MixinClass>()

    public fun registerMixins(mod: JarFile) {
        val weavinConfig = mod.getEntry("weavin.conf")

        BufferedReader(InputStreamReader(mod.getInputStream(weavinConfig))).use { reader ->
            for (line in reader.lines()) {
                val className = "$line.class"
                val classBytes = mod.getJarEntry(className).let { mod.getInputStream(it).readBytes() }

                val classNode = ClassNode()
                ClassReader(classBytes).accept(classNode, 0)

                val mixinAnnotation = classNode.visibleAnnotations?.find { it.desc == "Lclub/maxstats/weave/loader/api/mixin/Mixin;" }
                val targetClasspath = (mixinAnnotation?.values?.get(1) as? Type)?.className ?: continue

                this.mixins += MixinClass(targetClasspath.replace(".", "/"), classNode)
            }
        }
    }

    private fun createNode(annotationType: String, vararg value: Any?): AnnotationNode {
        val node = AnnotationNode(annotationType)
        (value.indices step 2).forEach { pos ->
            if (value[pos] !is String) {
                throw IllegalArgumentException("Annotation keys must be strings, found ${value[pos]?.javaClass?.simpleName} with ${value[pos]} at index $pos creating $annotationType")
            }
            node.visit(value[pos] as String, value[pos + 1])
        }
        return node
    }

    internal inner class MixinClass(val targetClasspath: String, val mixinClass: ClassNode) {
        fun applyMixin(node: ClassNode) {
            println("Applying mixin for " + node.name + " with " + mixinClass.name)

            for (method in mixinClass.methods) {
                println("checking annotations for " + method.name)

                val injectAnnotation = method.visibleAnnotations?.find { it.desc == "L${internalNameOf<Inject>()};" } ?: continue
                println("Found Inject Annotation")

                val injectValues = injectAnnotation.values.windowed(2, 2).associate { (k, v) -> k as String to v }
                val injectMethodValue = injectValues["method"] as String
                val injectAtValue = injectValues["at"] as AnnotationNode

                val atValues = injectAtValue.values.windowed(2, 2).associate { (k, v) -> k as String to v }
                val (_, loc) = atValues["value"] as Array<String>
                val atTargetValue = atValues["target"] as String? ?: ""
                val (_, shift) = atValues["shift"] as Array<String>
                val atByValue = atValues["by"] as Int? ?: 0

                println("Successfully retrieved all annotation values")

                val targetMethod = node.methods.named(injectMethodValue)
                /* HEAD by Default */
                var entryPoint = targetMethod.instructions.first
                when(loc) {
                    "INVOKE" -> {
                        val (className, methodString) = atTargetValue.split(";")
                        val (methodName, methodDesc) = methodString.split("(", limit = 2)
                        val methodDescFormatted = "($methodDesc"

                        entryPoint = targetMethod.instructions.find { it is MethodInsnNode && it.owner == className && it.name == methodName && it.desc == methodDescFormatted }
                    }
                    "FIELD" -> {
                        val (className, fieldName) = atTargetValue.split(";")
                        entryPoint = targetMethod.instructions.find { it is FieldInsnNode && it.owner == className && it.name == fieldName }
                    }
                    "HEAD" -> {
                        entryPoint = targetMethod.instructions.first
                    }
                    "RETURN" -> {
                        entryPoint = targetMethod.instructions.last
                    }
                }

                entryPoint = targetMethod.instructions.indexOf(entryPoint).let {
                    if (shift == "BEFORE") targetMethod.instructions[it - atByValue] else targetMethod.instructions[it + atByValue]
                }

                targetMethod.instructions.insertBefore(entryPoint, method.instructions)
            }
        }
    }

    internal inner class Transformer : SafeTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            originalClass: ByteArray
        ): ByteArray? {
            val mixins = mixins.filter { it.targetClasspath == className }

            if (mixins.isEmpty()) return originalClass

            println("Found Mixins for $className")
            val node = ClassNode()
            val reader = ClassReader(originalClass)
            reader.accept(node, 0)

            mixins.forEach { it.applyMixin(node) }

            val writer = object : ClassWriter(reader, COMPUTE_MAXS) {
                override fun getClassLoader() = loader
            }
            node.accept(writer)
            return writer.toByteArray()
        }
    }
}
