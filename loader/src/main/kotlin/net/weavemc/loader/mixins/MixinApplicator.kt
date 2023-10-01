package net.weavemc.loader.mixins

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.weavemc.loader.HookClassWriter
import net.weavemc.loader.MixinConfig
import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.mixin.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.jar.JarFile

class MixinApplicator {
    private val mixins = mutableListOf<MixinClass>()
    private var frozen = false
        private set(value) {
            if (!value) error("Cannot unfreeze mixin applicator!")
            field = true
        }
    private val json = Json { ignoreUnknownKeys = true }

    private fun registerMixin(classBytes: ByteArray) {
        if (frozen) error("Mixin registration is already frozen!")

        val classNode = ClassNode()
        ClassReader(classBytes).accept(classNode, 0)

        val mixinClass = Class.forName(classNode.name.replace("/", "."), false, MixinApplicator::class.java.classLoader)
        val mixinAnnotation = classNode.visibleAnnotations?.find { it.desc == "L${internalNameOf<Mixin>()};" }
            ?: error("Mixin class ${mixinClass.name} missing @Mixin annotation")
        val targetClasspath = (mixinAnnotation.values?.get(1) as? Type)?.className?.replace(".", "/")
            ?: error("Failed to parse @Mixin annotation. This should never happen!")

        println("Registered Mixin for $targetClasspath using ${mixinClass.name}")
        mixins += MixinClass(targetClasspath, mixinClass, classNode)
    }

    fun registerMixin(configPath: String, modJar: JarFile) {
        if (frozen) error("Mixin registration is already frozen!")

        val mixinConfig = json.decodeFromStream<MixinConfig>(modJar.getInputStream(modJar.getEntry(configPath)))

        mixinConfig.mixins.forEach { mixinClasspath ->
            val mixinClassBytes = modJar.getInputStream(
                modJar.getEntry("${mixinConfig.packagePath.replace(".", "/")}/$mixinClasspath.class")
            ).readBytes()

            registerMixin(mixinClassBytes)
        }
    }

    fun freeze() {
        if (!frozen) {
            frozen = true
        }
    }

    internal data class MixinClass(
        val targetClasspath: String,
        val mixinClass: Class<*>,
        private val mixinNode: ClassNode
    ) {
        private val callbackInfoType: Type = Type.getType(CallbackInfo::class.java)

        fun applyMixin(node: ClassNode) {
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
            val copiedMixinMethod = copyMixinToTarget(targetClass, mixinMethod)

            val targetMethod = targetClass.methods.find{ "${it.name}${it.desc}" == annotation.method }
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")
            val at = annotation.at

            val atInstruction: AbstractInsnNode = when (at.value) {
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
            } ?: error("Failed to find injection point $at")

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
                when (Type.getReturnType(copiedMixinMethod.desc)) {
                    Type.BOOLEAN_TYPE -> {
                        // Simplistic way to "cancel" a method using a function that returns a boolean
                        // This isn't required to create a cancellable mixin, just an alternative to using
                        // CallbackInfo.cancelled = true
                        // IMPORTANT! This will only work on targets that are of void type
                        aload(0)
                        invokevirtual(targetClass.name, copiedMixinMethod.name, copiedMixinMethod.desc)
                        val label = LabelNode()
                        ifeq(label)
                        _return
                        +label
                        f_same()
                    }
                    Type.VOID_TYPE -> {
                        aload(0)
                        invokevirtual(targetClass.name, copiedMixinMethod.name, copiedMixinMethod.desc)
                    }
                    else -> {
                        error("${copiedMixinMethod.name}'s return type is required to be either boolean or void. Ignoring mixin")
                    }
                }
            }

            // Passes parameters from the target method to the mixin method
            // If a mixin method uses any parameters other than CallbackInfo it must match the exact
            // parameter types and order of the target method
            // TODO check if arg has @Local annotation present, if so pass the local variable rather than searching for a matching argument
            val mixinArgTypes = Type.getArgumentTypes(copiedMixinMethod.desc)
            if (mixinArgTypes.isNotEmpty()) {
                val callbackInfo = targetMethod.maxLocals
                val targetArgTypes = Type.getArgumentTypes(targetMethod.desc)

                val targetArgs = arrayOfNulls<LocalVariable>(targetArgTypes.size)
                var stackLocation = 1
                for (i in targetArgTypes.indices) {
                    val targetArg = targetArgTypes[i]
                    val stackSize = targetArg.size

                    targetArgs[i] = LocalVariable(stackLocation, targetArg)
                    stackLocation += stackSize
                }

                val argsInsn = InsnList()
                for (i in mixinArgTypes.indices) {
                    val argType = mixinArgTypes[i]

                    val argIndex =
                        if (argType == callbackInfoType)
                            callbackInfo
                        else if (targetArgs[i]?.type == argType)
                            targetArgs[i]?.stackLocation
                        else {
                            null
                        } ?: error("Mismatch Parameters. ${copiedMixinMethod.desc} has parameter(s) that do not match ${targetMethod.desc}")

                    argsInsn.add(VarInsnNode(argType.getOpcode(Opcodes.ILOAD), argIndex))
                }
                // Insert after aload(0)
                insn.insert(insn.first, argsInsn)

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
                    aload(callbackInfo)
                    invokevirtual(
                        internalNameOf<CallbackInfo>(),
                        "getCancelled",
                        "()Z"
                    )

                    val label = LabelNode()
                    ifeq(label)

                    if (Type.getReturnType(copiedMixinMethod.desc) == Type.VOID_TYPE)
                        _return
                    else {
                        val returnType = Type.getReturnType(targetMethod.desc)
                        // CallbackInfo.returnValue is assumed to be the correct type to be returned.
                        invokevirtual(
                            internalNameOf<CallbackInfo>(),
                            "getReturnValue",
                            "()${returnType.descriptor};"
                        )
                        +InsnNode(returnType.getOpcode(Opcodes.IRETURN))
                    }

                    +label
                    f_same()
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

        private fun applyModifyArgs(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: ModifyArgs
        ) {

        }

        /**
         * Creates a method in the target class identical to the mixin method
         * The mixed in class will invoke this mixin method
         *
         * @param classNode Target class
         * @param method Mixin method
         */
        private fun copyMixinToTarget(classNode: ClassNode, method: MethodNode): MethodNode {
            return MethodNode(
                Opcodes.ACC_PUBLIC,
                "${method.name}_weaveMixin",
                method.desc,
                method.signature,
                method.exceptions.toTypedArray()
            ).also {
                it.instructions = method.instructions
                classNode.methods.add(it)
            }
        }
    }

    internal data class LocalVariable(
        val stackLocation: Int,
        val type: Type
    )

    internal inner class Transformer : SafeTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            originalClass: ByteArray
        ): ByteArray? {
            val applicableMixins = mixins.filter { it.targetClasspath == className }
            if (applicableMixins.isEmpty()) return null

            println("[Weave Mixin] Applying Mixins to $className")
            val node = ClassNode()
            val reader = ClassReader(originalClass)
            reader.accept(node, 0)

            applicableMixins.forEach {
                it.applyMixin(node)
                println("  - ${it.mixinClass.name}")
            }

            val writer = HookClassWriter(reader, ClassWriter.COMPUTE_FRAMES)

            node.accept(writer)
            return writer.toByteArray()
        }
    }
}
