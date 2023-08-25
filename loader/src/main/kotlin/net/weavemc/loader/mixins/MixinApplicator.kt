package net.weavemc.loader.mixins

import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.mixin.At
import net.weavemc.weave.api.mixin.Inject
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

public class MixinApplicator {
    private val mixins = mutableListOf<MixinClass>()
    private var frozen = false
        private set(value) {
            if (!value) error("Cannot unfreeze mixin applicator!")
            field = true
        }

    private val frozenLookup by lazy { mixins.groupBy { it.targetClasspath } }

    public fun registerMixins(classBytes: ByteArray) {
        if (frozen) error("Mixin registration is already frozen!")

        val classNode = ClassNode()
        ClassReader(classBytes).accept(classNode, 0)

        val mixinClass = Class.forName(classNode.name.replace("/", "."), false, MixinApplicator::class.java.classLoader)
        val mixinAnnotation = classNode.visibleAnnotations?.find { it.desc == "Lclub/maxstats/weave/loader/api/mixin/Mixin;" }
        val targetClasspath = (mixinAnnotation?.values?.get(1) as? Type)?.className?.replace(".", "/")
            ?: error("Failed to parse @Mixin annotation. This should never happen!")

        mixins += MixinClass(targetClasspath, mixinClass, classNode)
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
                val targetMethod = node.methods.find{ it.name == injectAnnotation.method }
                    ?: error("Failed to load Mixin ${mixinClass.name}: ${injectAnnotation.method} is not present in class ${node.name}")
                val at = injectAnnotation.at

                val entryPoint = when (at.value) {
                    At.Location.INVOKE -> {
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

                    At.Location.PUTFIELD -> {
                        val (className, fieldName) = at.target.split(";")
                        targetMethod.instructions.find {
                            it is FieldInsnNode && it.opcode == Opcodes.PUTFIELD && it.owner == className && it.name == fieldName
                        }
                    }

                    At.Location.GETFIELD -> {
                        val (className, fieldName) = at.target.split(";")
                        targetMethod.instructions.find {
                            it is FieldInsnNode && it.opcode == Opcodes.GETFIELD && it.owner == className && it.name == fieldName
                        }
                    }

                    At.Location.RETURN -> targetMethod.instructions.findLast { it.opcode == Type.getReturnType(targetMethod.desc).getOpcode(
                        Opcodes.IRETURN) }
                    At.Location.HEAD -> targetMethod.instructions.first
                }

                val index = targetMethod.instructions.indexOf(entryPoint)
                val shiftedIndex = if (at.shift == At.Shift.BEFORE) index - at.by else index + at.by + 1
                val injectionPoint = targetMethod.instructions[shiftedIndex]

                val insn: InsnList =  asm {
                    when (rawMethod.desc.substring(rawMethod.desc.lastIndexOf(")") + 1)) {
                        "Z" -> {
                            invokestatic(mixinNode.name, rawMethod.name, rawMethod.desc)
                            val label = LabelNode()
                            ifeq(label)
//                            returnCorrect(targetMethod.desc)
                            +label
                        }
                        "V" -> invokestatic(mixinNode.name, rawMethod.name, rawMethod.desc)
                        else -> {
                            error("${rawMethod.name}'s return type is required to be either boolean or void. Ignoring mixin")
                        }
                    }
                }

                val mixinArgTypes = Type.getArgumentTypes(rawMethod.desc)
                if (mixinArgTypes.isNotEmpty()) {
                    val targetArgTypes = Type.getArgumentTypes(targetMethod.desc)

                    val argIndices = mutableMapOf<Type, Int>()
                    for (i in targetArgTypes.indices)
                        argIndices[targetArgTypes[i]] = i + 1

                    val argsInsn = InsnList()
                    for (argType in mixinArgTypes) {
                        val argIndex = argIndices[argType]
                            ?: error("Mismatch Parameters. ${rawMethod.desc} has parameter(s) that do not match ${targetMethod.desc}");

                        argsInsn.add(VarInsnNode(argType.getOpcode(Opcodes.ILOAD), argIndex))
                    }
                    insn.insert(argsInsn)
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
