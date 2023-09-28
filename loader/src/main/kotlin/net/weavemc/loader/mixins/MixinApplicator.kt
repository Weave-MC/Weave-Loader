package net.weavemc.loader.mixins

import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.mixin.At
import net.weavemc.weave.api.mixin.CallbackInfo
import net.weavemc.weave.api.mixin.Inject
import net.weavemc.weave.api.mixin.Overwrite
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.reflect.Method

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
        private val callbackInfoType: Type = Type.getType(CallbackInfo::class.java)

        fun applyMixin(node: ClassNode) {
            println("Applying mixin for ${node.name} with ${mixinClass.name}")

            for (method in mixinClass.declaredMethods) {
                val rawMethod = mixinNode.methods.find {
                    it.name == method.name && it.desc == Type.getMethodDescriptor(method)
                } ?: error("No matching raw method found for $mixinClass, should never happen")

                when {
                    method.isAnnotationPresent(Inject::class.java) -> applyInjection(
                        node, mixinNode, rawMethod, method.getAnnotation(Inject::class.java)
                    )
                    method.isAnnotationPresent(Overwrite::class.java) -> applyOverwrite(
                        node, rawMethod, method.getAnnotation(Overwrite::class.java)
                    )
                }
            }
        }
        private fun applyInjection(
            targetClass: ClassNode,
            mixinClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: Inject
        ) {
            val targetMethod = targetClass.methods.find{ it.name == annotation.method }
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")
            val at = annotation.at

            val atInstruction: AbstractInsnNode? = when (at.value) {
                At.Location.HEAD -> targetMethod.instructions.first
                At.Location.TAIL -> {
                    targetMethod.instructions.findLast {
                        it.opcode == Type.getReturnType(targetMethod.desc).getOpcode(Opcodes.IRETURN)
                    }
                }
                At.Location.OPCODE -> {
                    when (at.opcode) {
                        Opcodes.GETFIELD, Opcodes.PUTFIELD -> {
                            val (className, fieldName) = at.target.split(";")
                            targetMethod.instructions.find {
                                it is FieldInsnNode &&
                                    it.opcode == at.opcode &&
                                    it.owner == className &&
                                    it.name == fieldName
                            }
                        }
                        Opcodes.INVOKESTATIC, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEDYNAMIC, Opcodes.INVOKESPECIAL -> {
                            val (className, methodString) = at.target.split(";")
                            val (methodName, methodDesc) = methodString.split("(", limit = 2)
                            val methodDescFormatted = "($methodDesc"
                            targetMethod.instructions.find {
                                it is MethodInsnNode &&
                                    it.opcode == at.opcode &&
                                    it.owner == className &&
                                    it.name == methodName &&
                                    it.desc == methodDescFormatted
                            }
                        }
                        Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.DLOAD, Opcodes.FLOAD,
                        Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.DSTORE, Opcodes.FSTORE -> {
                            targetMethod.instructions.find {
                                it is VarInsnNode &&
                                    it.opcode == at.opcode &&
                                    it.`var` == at.target.toInt()
                            }
                        }
                        Opcodes.LDC -> {
                            targetMethod.instructions.find {
                                it is LdcInsnNode &&
                                    it.opcode == at.opcode &&
                                    it.cst == at.target
                            }
                        }
                        Opcodes.JSR, Opcodes.GOTO, Opcodes.IFEQ, Opcodes.IFNE,
                        Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
                        Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT,
                        Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE,
                        Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.IFNULL,
                        Opcodes.IFNONNULL -> {
                            targetMethod.instructions.find {
                                it is JumpInsnNode && it.opcode == at.opcode
                            }
                        }
                        else -> targetMethod.instructions.find { it.opcode == at.opcode }
                    }
                }
                else -> targetMethod.instructions.first
            }

            val index = targetMethod.instructions.indexOf(atInstruction)
            val shiftedIndex = when (at.shift) {
                At.Shift.BEFORE -> {
                    index - at.by
                }
                At.Shift.AFTER -> {
                    index + at.by + 1
                }
                else -> index
            }
            val injectionPoint = targetMethod.instructions[shiftedIndex]

            val insn: InsnList =  asm {
                when (Type.getReturnType(mixinMethod.desc)) {
                    Type.BOOLEAN_TYPE -> {
                        // Simplistic way to create a "cancel" a method using a function that returns a boolean
                        // This isn't required to create a cancellable mixin, just an alternative to using
                        // CallbackInfo.cancelled = true
                        // IMPORTANT! This will only work on targets that are of void type
                        invokestatic(mixinClass.name, mixinMethod.name, mixinMethod.desc)
                        val label = LabelNode()
                        ifeq(label)
                        _return
                        +label
                    }
                    Type.VOID_TYPE -> invokestatic(mixinClass.name, mixinMethod.name, mixinMethod.desc)
                    else -> {
                        error("${mixinMethod.name}'s return type is required to be either boolean or void. Ignoring mixin")
                    }
                }
            }

            // Passes parameters from the target method to the mixin method
            // If a mixin method uses any parameters other than CallbackInfo it must match the exact
            // parameter types and order of the target method
            // TODO check if arg has @Local annotation present, if so pass the local variable rather than searching for a matching argument
            val mixinArgTypes = Type.getArgumentTypes(mixinMethod.desc)
            if (mixinArgTypes.isNotEmpty()) {
                val callbackInfo = targetMethod.maxLocals
                val targetArgTypes = Type.getArgumentTypes(targetMethod.desc)

                val argIndices = mutableMapOf<Type, Int>()
                for (i in targetArgTypes.indices)
                    argIndices[targetArgTypes[i]] = i + 1

                val argsInsn = InsnList()
                for (argType in mixinArgTypes) {
                    val argIndex = if (argType == callbackInfoType) callbackInfo else argIndices[argType]
                        ?: error("Mismatch Parameters. ${mixinMethod.desc} has parameter(s) that do not match ${targetMethod.desc}");

                    argsInsn.add(VarInsnNode(argType.getOpcode(Opcodes.ILOAD), argIndex))
                }
                insn.insert(argsInsn)

                // Create CallbackInfo instance to be passed to our mixin method
                insn.insert(asm {
                    new(internalNameOf<CallbackInfo>())
                    dup
                    invokespecial(
                        internalNameOf<CallbackInfo>(),
                        "<init>",
                        "()V"
                    )
                    astore(callbackInfo)
                })

                insn.add(asm {
                    if (Type.getReturnType(mixinMethod.desc) == Type.VOID_TYPE) {
                        val label = LabelNode()
                        ifeq(label)
                        _return
                        +label
                    } else {
                        val returnType = Type.getReturnType(targetMethod.desc)
                        // CallbackInfo.returnValue is assumed to be the correct type to be returned.
                        aload(callbackInfo)
                        invokevirtual(
                            internalNameOf<CallbackInfo>(),
                            "getCancelled",
                            "()Z"
                        )
                        val label = LabelNode()
                        ifeq(label)
                        invokevirtual(
                            internalNameOf<CallbackInfo>(),
                            "getReturnValue",
                            "()${returnType.descriptor};"
                        )
                        +InsnNode(returnType.getOpcode(Opcodes.IRETURN))
                        +label
                    }
                })
            }

            targetMethod.instructions.insertBefore(injectionPoint, insn)
        }

        private fun applyOverwrite(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: Overwrite
        ) {
            targetClass.methods.find{ it.name == annotation.method }?.instructions = mixinMethod.instructions
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")
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
