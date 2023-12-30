package net.weavemc.loader.mixins

import com.grappenmaker.mappings.MappingsRemapper
import net.weavemc.loader.*
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.mixin.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_INTERFACE
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

    private fun registerMixin(remapper: MappingsRemapper, classBytes: ByteArray) {
        if (frozen) error("Mixin registration is already frozen!")

        val classNode = ClassNode()
        ClassReader(classBytes).accept(classNode, 0)

        val mixinAnnotation = classNode.visibleAnnotations?.find { it.desc == "L${internalNameOf<Mixin>()};" }
            ?: error("Mixin class ${classNode.name} missing @Mixin annotation")

        val targetClasspath = (mixinAnnotation.values?.get(1) as? Type)?.className?.replace('.', '/')
            ?: error("Failed to parse @Mixin annotation. This should never happen!")

        val mappedTargetClasspath = remapper.map(targetClasspath)

        println("Registered Mixin for $mappedTargetClasspath using ${classNode.name}")
        mixins += MixinClass(mappedTargetClasspath, classNode)
    }

    fun registerMixin(mixinConfigPath: String, modJar: JarFile, remapper: MappingsRemapper) {
        if (frozen) error("Mixin registration is already frozen!")

        val mixinConfig = modJar.fetchMixinConfig(mixinConfigPath, JSON)
        mixinConfig.mixins.forEach { mixinClasspath ->
            val mixinClassBytes = modJar.getInputStream(
                modJar.getEntry("${mixinConfig.packagePath.replace(".", "/")}/$mixinClasspath.class")
            ).readBytes()

            registerMixin(remapper, mixinClassBytes)
        }
    }

    fun freeze() {
        frozen = true
    }

    internal data class MixinClass(
        val targetClasspath: String,
        val mixinNode: ClassNode
    ) {
        private val callbackInfoType: Type = Type.getType(CallbackInfo::class.java)

        fun applyMixin(node: ClassNode) {
            val shadowed = mutableListOf<Pair<String, String>>()

            mixinNode.fields.forEach { f -> f.useAnnotation<Shadow> { shadowed += f.name to f.desc } }

            for (method in mixinNode.methods) {
                method.useAnnotation<Inject> {
                    applyInjection(node, mixinNode, method, method.parameters?.toList() ?: emptyList(), it)
                }

                method.useAnnotation<Overwrite> { applyOverwrite(node, method, it) }
                method.useAnnotation<ModifyArgs> { applyModifyArgs(node, method, it) }
                method.useAnnotation<ModifyConstant> { applyModifyConstant(node, method, it) }
                method.useAnnotation<Accessor> {
                    if (mixinNode.access and ACC_INTERFACE == 0)
                        error("Accessor annotation found in non-interface Mixin class ${mixinNode.name}")

                    applyAccessor(node, method, it)
                }

                method.useAnnotation<Invoker> {
                    if (mixinNode.access and ACC_INTERFACE == 0)
                        error("Accessor annotation found in non-interface Mixin class ${mixinNode.name}")

                    applyInvoker(node, method)
                }

                method.useAnnotation<Redirect> { applyRedirect(node, method, it) }
                method.useAnnotation<Shadow> { shadowed += method.name to method.desc }
            }

            node.methods.forEach { it.remapShadow(node.name, shadowed) }
        }

        private fun MethodNode.remapShadow(to: String, shadows: List<Pair<String, String>>) {
            val shadowRemapper = object : MethodVisitor(Opcodes.ASM9, this) {
                override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                    shadows.find { it.first == name && it.second == descriptor }
                        ?: super.visitFieldInsn(opcode, owner, name, descriptor)

                    super.visitFieldInsn(opcode, to, name, descriptor)
                }

                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String?,
                    name: String?,
                    descriptor: String?,
                    isInterface: Boolean
                ) {
                    shadows.find { it.first == name && it.second == descriptor }
                        ?: super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

                    super.visitMethodInsn(opcode, to, name, descriptor, isInterface)
                }
            }

            this.accept(shadowRemapper)
        }

        private fun applyInjection(
            targetClass: ClassNode,
            mixinClass: ClassNode,
            mixinMethod: MethodNode,
            mixinMethodParams: List<ParameterNode>,
            annotation: Inject,
        ): MethodNode {
            val targetMethod = targetClass.methods.find { "${it.name}${it.desc}" == annotation.method }
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")

            val copiedMixinMethod = copyMixinToTarget(targetClass, mixinMethod)
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
            val insn = asm {
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

                    else -> error("${copiedMixinMethod.name}'s return type is required to be either boolean or void")
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

                    val argIndex = when {
                        argType == callbackInfoType -> callbackInfo
                        targetArgs[i]?.type == argType -> targetArgs[i]?.stackLocation
//                        i < mixinMethodParams.size && mixinMethodParams[i].isAnnotationPresent(Local::class.java) ->
//                            mixinMethodParams[i].getAnnotation(Local::class.java).ordinal

                        else -> null
                    } ?: error("Mismatch Parameters. ${copiedMixinMethod.desc} has parameter(s) that do not match ${targetMethod.desc}")

                    argsInsn.add(VarInsnNode(argType.getOpcode(Opcodes.ILOAD), argIndex))
                }
                // Insert after aload(0) (which in this case is insn.first)
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

                // Add cancelled if block
                insn.add(asm {
                    aload(callbackInfo)
                    invokevirtual(
                        internalNameOf<CallbackInfo>(),
                        "getCancelled",
                        "()Z"
                    )

                    val label = LabelNode()
                    ifeq(label)

                    if (Type.getReturnType(copiedMixinMethod.desc) == Type.VOID_TYPE) _return else {
                        //TODO fix this
                        val returnType = Type.getReturnType(targetMethod.desc)
                        // CallbackInfo.returnValue is assumed to be the correct type to be returned.
                        invokevirtual(
                            internalNameOf<CallbackInfo>(),
                            "getReturnValue",
                            "()Ljava/lang/Object;"
                        )

                        +InsnNode(returnType.getOpcode(Opcodes.IRETURN))
                    }

                    +label
                    f_same()
                })
            }

            targetMethod.instructions.insertBefore(injectionPoint, insn)
            return copiedMixinMethod
        }

        private fun applyOverwrite(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: Overwrite
        ) = targetClass.methods.find { it.name == annotation.method }
            ?.also { it.instructions = mixinMethod.instructions }
            ?: error("Failed to load Mixin ${mixinNode.name}: ${annotation.method} is not present in class ${targetClass.name}")

        private fun applyModifyArgs(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: ModifyArgs
        ) = mixinMethod

        private fun applyModifyConstant(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: ModifyConstant
        ) = mixinMethod

        private fun applyAccessor(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: Accessor
        ): MethodNode {
            if (!targetClass.interfaces.any { it == mixinNode.name })
                targetClass.interfaces.add(mixinNode.name)

            val target = annotation.field
            val accessedField = targetClass.fields.find { it.name == target }
                ?: error("Failed to find $target field in ${targetClass.name}")

            val argTypes = Type.getArgumentTypes(mixinMethod.desc)
            return if (argTypes.isNotEmpty()) when {
                (accessedField.access and Opcodes.ACC_FINAL) != 0 ->
                    error("Cannot create Accessor setter for a final field: $target in ${targetClass.name}")

                argTypes.size > 1 -> error("Accessor setters should only have a single parameter")

                else -> MethodNode(
                    Opcodes.ACC_PUBLIC,
                    mixinMethod.name,
                    mixinMethod.desc,
                    null,
                    mixinMethod.exceptions.toTypedArray()
                ).also {
                    it.instructions = asm {
                        aload(0)
                        +VarInsnNode(argTypes[0].getOpcode(Opcodes.ILOAD), 1)
                        putfield(
                            targetClass.name,
                            accessedField.name,
                            accessedField.desc
                        )

                        _return
                    }

                    targetClass.methods.add(it)
                }
            } else MethodNode(
                Opcodes.ACC_PUBLIC,
                mixinMethod.name,
                mixinMethod.desc,
                null,
                mixinMethod.exceptions.toTypedArray()
            ).also {
                it.instructions = asm {
                    aload(0)
                    getfield(
                        targetClass.name,
                        accessedField.name,
                        accessedField.desc
                    )

                    +InsnNode(Type.getReturnType(accessedField.desc).getOpcode(Opcodes.IRETURN))
                }

                targetClass.methods.add(it)
            }
        }

        private fun applyInvoker(
            targetClass: ClassNode,
            mixinMethod: MethodNode
        ) = targetClass.methods
            .find { it.desc == mixinMethod.desc }
            .also { targetClass.interfaces.add(mixinNode.name) }
            ?: error("Failed to find method with matching descriptor: ${mixinMethod.desc} in ${targetClass.name}")

        private fun applyRedirect(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: Redirect
        ) = mixinMethod

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
                "",
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

            val originalNode = ClassNode()
            val reader = ClassReader(originalClass)
            reader.accept(originalNode, 0)

            val writer = HookClassWriter(ClassWriter.COMPUTE_FRAMES)

            applicableMixins.forEach { mixin ->
                println("  - ${mixin.mixinNode.name}")
                mixin.applyMixin(originalNode)
            }

            originalNode.accept(writer)
            return writer.toByteArray()
        }
    }
}

private inline fun <reified T : Annotation> MethodNode.getAnnotation(): T? {
    val desc = "L${internalNameOf<T>()};"
    return visibleAnnotations?.find { it.desc == desc }?.reflect<T>()
}

private inline fun <reified T : Annotation> MethodNode.useAnnotation(block: (T) -> Unit) =
    getAnnotation<T>()?.let(block)

private inline fun <reified T : Annotation> FieldNode.getAnnotation(): T? {
    val desc = "L${internalNameOf<T>()};"
    return visibleAnnotations?.find { it.desc == desc }?.reflect<T>()
}

private inline fun <reified T : Annotation> FieldNode.useAnnotation(block: (T) -> Unit) =
    getAnnotation<T>()?.let(block)