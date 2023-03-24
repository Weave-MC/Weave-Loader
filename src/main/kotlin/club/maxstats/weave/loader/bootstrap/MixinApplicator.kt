package club.maxstats.weave.loader.bootstrap

import club.maxstats.weave.loader.api.mixin.At
import club.maxstats.weave.loader.api.mixin.At.Location.*
import club.maxstats.weave.loader.api.mixin.Inject
import club.maxstats.weave.loader.api.util.asm
import club.maxstats.weave.loader.util.named
import club.maxstats.weave.loader.util.returnCorrect
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.jar.JarFile

public class MixinApplicator {
    private val mixins = mutableListOf<MixinClass>()
    private var frozen = false
        private set(value) {
            if (!value) error("Cannot unfreeze mixin applicator!")
            field = true
        }

    private val frozenLookup by lazy { mixins.groupBy { it.targetClasspath } }

    public fun registerMixins(mod: JarFile) {
        if (frozen) error("Mixin registration is already frozen!")

        val lines = mod.getInputStream(mod.getEntry("weavin.conf")).bufferedReader().readLines()

        for (line in lines) {
            val className = "$line.class"
            val classBytes = mod.getInputStream(mod.getJarEntry(className)).readBytes()

            val classNode = ClassNode()
            ClassReader(classBytes).accept(classNode, 0)

            val mixinClass = Class.forName(className.replace(".class", "").replace("/", "."), false, MixinApplicator::class.java.classLoader)
            val mixinAnnotation = classNode.visibleAnnotations?.find { it.desc == "Lclub/maxstats/weave/loader/api/mixin/Mixin;" }
            val targetClasspath = (mixinAnnotation?.values?.get(1) as? Type)?.className?.replace(".", "/") ?: continue

            mixins += MixinClass(targetClasspath, mixinClass, classNode)
        }
    }

    public fun freeze() {
        if (!frozen) {
            // Reference frozenLookup once to "freeze" it
            frozenLookup
            frozen = true
        }
    }

    internal data class MixinClass(
        val targetClasspath: String,
        private val mixinClass: Class<*>,
        private val mixinNode: ClassNode
    ) {
        fun applyMixin(node: ClassNode) {
            println("Applying mixin for ${node.name} with ${mixinClass.name}")

            for (method in mixinClass.declaredMethods) {
                val rawMethod = mixinNode.methods.find {
                    it.name == method.name && it.desc == Type.getMethodDescriptor(method)
                } ?: error("No matching raw method found for $mixinClass, should never happen")

                val injectAnnotation = method.getAnnotation(Inject::class.java) ?: continue
                val targetMethod = node.methods.named(injectAnnotation.method)
                val at = injectAnnotation.at

                val entryPoint = when (at.value) {
                    INVOKE -> {
                        val (className, methodString) = at.target.split(";")
                        val (methodName, methodDesc) = methodString.split("(", limit = 2)
                        val methodDescFormatted = "($methodDesc"
                        targetMethod.instructions.find {
                            it is MethodInsnNode &&
                                    it.owner == className &&
                                    it.name == methodName &&
                                    it.desc == methodDescFormatted
                        }
                    }

                    FIELD -> {
                        val (className, fieldName) = at.target.split(";")
                        targetMethod.instructions.find {
                            it is FieldInsnNode && it.owner == className && it.name == fieldName
                        }
                    }

                    RETURN -> targetMethod.instructions.last
                    HEAD -> targetMethod.instructions.first
                }

                val index = targetMethod.instructions.indexOf(entryPoint)
                val shiftedIndex = if (at.shift == At.Shift.BEFORE) index - at.by else index + at.by
                val injectionPoint = targetMethod.instructions[shiftedIndex]

                val insn: InsnList =  asm {
                    when (rawMethod.desc.substring(rawMethod.desc.lastIndexOf(")") + 1)) {
                        "Z" -> {
                            invokestatic(mixinNode.name, rawMethod.name, rawMethod.desc)
                            val label = LabelNode()
                            ifeq(label)
                            returnCorrect(targetMethod.desc)
                            +label
                        }
                        "V" -> invokestatic(mixinNode.name, rawMethod.name, rawMethod.desc)
                        else -> {
                            error("${rawMethod.name}'s return type is required to be either boolean or void. Ignoring mixin")
                        }
                    }
                }

                targetMethod.instructions.insertBefore(injectionPoint, insn)
            }
        }
    }

    internal inner class Transformer : SafeTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            originalClass: ByteArray
        ): ByteArray? {
            val applicableMixins = frozenLookup[className] ?: return null
            if (applicableMixins.isEmpty()) return null

            val node = ClassNode()
            val reader = ClassReader(originalClass)
            reader.accept(node, 0)

            applicableMixins.forEach { it.applyMixin(node) }

            val writer = object : ClassWriter(reader, COMPUTE_MAXS) {
                override fun getClassLoader() = loader
            }

            node.accept(writer)
            return writer.toByteArray()
        }
    }
}

//internal val AnnotationNode.mapped get() = values.windowed(2, 2).associate { (k, v) -> k as String to v }
//internal inline fun <reified T : Enum<T>> AnnotationNode.getEnum(name: String) =
//    ((mapped[name] as Array<*>?)?.get(1) as? String?)?.let { enumValueOf<T>(it) }
