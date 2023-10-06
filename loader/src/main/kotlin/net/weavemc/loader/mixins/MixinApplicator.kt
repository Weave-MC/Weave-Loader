package net.weavemc.loader.mixins

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.weavemc.loader.HookClassWriter
import net.weavemc.loader.MixinConfig
import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.bytecode.InsnBuilder
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.dump
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.mixin.*
import net.weavemc.weave.api.mixin.Constant.ConstantType
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.reflect.Parameter
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
            node.remapToMCP()

            val mixedMethods: ArrayList<MethodNode> = arrayListOf()
            val shadowed: ArrayList<Pair<String, String>> = arrayListOf()

            mixinClass.declaredFields
                .filter { it.isAnnotationPresent(Shadow::class.java) }
                .forEach { shadowed += it.name to it.type.name }

            for (method in mixinClass.declaredMethods) {
                val rawMethod = mixinNode.methods.find {
                    it.name == method.name && it.desc == Type.getMethodDescriptor(method)
                } ?: error("No matching raw method found for $mixinClass, should never happen")

                //TODO find a way to clean this up
                mixedMethods += when {
                    method.isAnnotationPresent(Inject::class.java) -> applyInjection(
                        node, mixinNode, rawMethod, method.parameters, method.getAnnotation(Inject::class.java)
                    )

                    method.isAnnotationPresent(Overwrite::class.java) -> applyOverwrite(
                        node, rawMethod, method.getAnnotation(Overwrite::class.java)
                    )

                    method.isAnnotationPresent(ModifyConstant::class.java) -> applyModifyConstant(
                        node, rawMethod, method.getAnnotation(ModifyConstant::class.java)
                    )

                    method.isAnnotationPresent(Accessor::class.java) -> {
                        if (!mixinClass.isInterface)
                            error("Accessor annotation found in non-interface Mixin class ${mixinClass.name}")

                        applyAccessor(
                            node, mixinClass, rawMethod, method.getAnnotation(Accessor::class.java)
                        )
                    }

                    method.isAnnotationPresent(Invoker::class.java) -> {
                        if (!mixinClass.isInterface)
                            error("Invoker annotation found in non-interface Mixin class ${mixinClass.name}")

                        applyInvoker(
                            node, mixinClass, rawMethod
                        )
                    }

                    method.isAnnotationPresent(Redirect::class.java) -> applyRedirect(
                        node, rawMethod, method.getAnnotation(Redirect::class.java)
                    )

                    method.isAnnotationPresent(Shadow::class.java) -> {
                        shadowed += rawMethod.name to rawMethod.desc
                        continue
                    }

                    else -> continue
                }

                mixedMethods += when {
                    method.isAnnotationPresent(ModifyArg::class.java) -> applyModifyArg(
                        node, rawMethod, method.getAnnotation(ModifyArg::class.java)
                    )

                    method.isAnnotationPresent(ModifyArgs::class.java) -> applyModifyArgs(
                        node, rawMethod, method.getAnnotation(ModifyArgs::class.java)
                    )

                    else -> continue
                }
            }

            mixedMethods.forEach { it.remapShadowed() }
            node.remapToNormal()
        }

        // TODO probably move this to a utility class to reduce size of this file

        /**
         * Remaps the ClassNode to MCP mappings to match with the mod's mixin
         */
        fun ClassNode.remapToMCP() {

        }

        /**
         * Remaps the ClassNode to its normal mappings
         * Should be called after the mixin is applied so that the mixin is mapped as well
         */
        fun ClassNode.remapToNormal() {

        }

        fun MethodNode.remapShadowed() {

        }

        private fun applyInjection(
            targetClass: ClassNode,
            mixinClass: ClassNode,
            mixinMethod: MethodNode,
            mixinMethodParams: Array<Parameter>,
            annotation: Inject,
        ): MethodNode {
            val targetMethod = targetClass.methods.find{ "${it.name}${it.desc}" == annotation.method }
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
                        error("${copiedMixinMethod.name}'s return type is required to be either boolean or void")
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

                    val argIndex = when {
                        argType == callbackInfoType -> callbackInfo
                        targetArgs[i]?.type == argType ->
                            targetArgs[i]?.stackLocation
                        i < mixinMethodParams.size && mixinMethodParams[i].isAnnotationPresent(Local::class.java) ->
                            mixinMethodParams[i].getAnnotation(Local::class.java).ordinal
                        else ->
                            null
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
            return copiedMixinMethod
        }

        private fun applyOverwrite(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: Overwrite
        ): MethodNode {
            return targetClass.methods.find { it.name == annotation.method }?.also { it.instructions = mixinMethod.instructions }
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")
        }

        /**
         * (1) Copies the mixin method to the target class.
         * (2) Creates a new method in the target class that has the same descriptor as the invoked method that loads all of its parameters and when the index of the parameter is the same as the specified index, it invokes the copied mixin method. Lastly, it invokes the method that was originally invoked.
         * (3) Replaces the invoked method with the newly created method.
         */
        private fun applyModifyArg(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: ModifyArg
        ): List<MethodNode> {
            val targetMethod = targetClass.methods.find { "${it.name}${it.desc}" == annotation.method }
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")

            val invokedMethod = targetMethod.instructions
                .filterIsInstance<MethodInsnNode>()
                .filter { it.name == annotation.invokedMethod }
                .getOrNull(annotation.shift)
                ?: error("Failed to find invoked method ${annotation.invokedMethod} Either the method does not exist or the index is out of bounds (targetClass: ${targetClass.name}, method: ${targetMethod.name}, annotation: ${annotation.id})")
            val copiedMethod = copyMixinToTarget(targetClass, mixinMethod)

            val isStatic = targetMethod.access and Opcodes.ACC_STATIC != 0

            // if static start at 0, else start at 1
            var index = if (isStatic) 0 else 1

            val argIndex = annotation.index + index

            val loads = asm {
                val parametersStrings = targetMethod.parametersStrings ?: error("Failed to find parameterStrings for ${targetMethod.name} in ${targetClass.name} when applying @ModifyArg (mixinClass: ${mixinClass.name}, method: ${mixinMethod.name}, annotation: ${annotation.id})")
                parametersStrings.forEachIndexed { i, parameterString ->
                    index += load(index, parameterString)

                    if (i == argIndex) {
                        if (isStatic) {
                            invokestatic(targetClass.name, copiedMethod.name, copiedMethod.desc)
                        } else {
                            aload(0)
                            swap
                            invokevirtual(targetClass.name, copiedMethod.name, copiedMethod.desc)
                        }
                    }
                }
            }

            val returnInstructions = setOf(
                Opcodes.IRETURN,
                Opcodes.LRETURN,
                Opcodes.FRETURN,
                Opcodes.DRETURN,
                Opcodes.ARETURN,
            )
            var returnInstruction: AbstractInsnNode? = null
            for (r in returnInstructions) {
                val mixinReturnInstruction = mixinMethod.instructions.find { it.opcode == r }
                if (mixinReturnInstruction != null) {
                    returnInstruction = mixinReturnInstruction
                    break
                }
            }

            val generatedMethod = MethodNode(
                if (isStatic) Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC else Opcodes.ACC_PUBLIC,
                "${targetMethod.name}_weaveMixin\$ModifyArg",
                mixinMethod.desc,
                null,
                null
            ).apply {
                instructions = loads.apply {
                    if (invokedMethod.opcode != Opcodes.INVOKESTATIC) {
                        insert(VarInsnNode(Opcodes.ALOAD, 0))
                    }
                    add(MethodInsnNode(invokedMethod.opcode, invokedMethod.owner, invokedMethod.name, invokedMethod.desc, invokedMethod.itf))
                    add(returnInstruction)
                }
                targetClass.methods.add(this)
            }

            return listOf(targetMethod, generatedMethod)
        }

        /**
         * (1) Copies the mixin method to the target class.
         * (2) Creates a new method in the target class that has the same descriptor as the invoked method that creates an instance of [Args] and invokes the copied mixin method with the [Args] instance. Lastly, it gets all the parameters from the [Args] instance and invokes the method that was originally invoked.
         * (3) Replaces the invoked method with the newly created method.
         */
        private fun applyModifyArgs(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: ModifyArgs
        ): List<MethodNode> {
            val targetMethod = targetClass.methods.find { "${it.name}${it.desc}" == annotation.method }
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")

            val invokedMethod = targetMethod.instructions
                .filterIsInstance<MethodInsnNode>()
                .filter { it.name == annotation.invokedMethod }
                .getOrNull(annotation.shift)
                ?: error("Failed to find invoked method ${annotation.invokedMethod} Either the method does not exist or the index is out of bounds (targetClass: ${targetClass.name}, method: ${targetMethod.name}, annotation: ${annotation.id})")
            val copiedMethod = copyMixinToTarget(targetClass, mixinMethod)

            val isStatic = targetMethod.access and Opcodes.ACC_STATIC != 0

            // if static start at 0, else start at 1
            var index = if (isStatic) 0 else 1

            val loads = asm {
                val parametersStrings = targetMethod.parametersStrings ?: error("Failed to find parameterStrings for ${targetMethod.name} in ${targetClass.name} when applying @ModifyArg (mixinClass: ${mixinClass.name}, method: ${mixinMethod.name}, annotation: ${annotation.id})")

                // creates an instance of Args which accepts an array of objects which are the parameters of the target method
                new(internalNameOf<Args>())
                dup // Args
                value(parametersStrings.size)
                anewarray(internalNameOf<Any>())
                parametersStrings.forEachIndexed { i, parametersString ->
                    dup // array reference
                    value(i)
                    index += load(index, parametersString)
                    boxNoException(Type.getType(parametersString))
                    aastore
                }
                invokespecial(internalNameOf<Args>(), "<init>", "([L${internalNameOf<Any>()};)V")

                // invokes the copied mixin method with the Args instance
                dup // Args
                if (isStatic) {
                    invokestatic(targetClass.name, copiedMethod.name, copiedMethod.desc)
                } else {
                    aload(0)
                    swap
                    invokevirtual(targetClass.name, copiedMethod.name, copiedMethod.desc)
                }

                // stores the returned Args instance in a local variable
                if (isStatic) {
                    astore(parametersStrings.size)
                } else {
                    // prepares "this" for the invocation of the target method
                    aload(0)
                    astore(parametersStrings.size + 1)
                }

                // gets all the parameters from the Args instance
                parametersStrings.forEachIndexed { i, parametersString ->
                    // loads the Args instance
                    if (isStatic) {
                        aload(parametersStrings.size)
                    } else {
                        aload(parametersStrings.size + 1)
                    }
                    value(i)
                    invokevirtual(internalNameOf<Args>(), "get", "(I)L${internalNameOf<Any>()};")
                    unboxNoException(Type.getType(parametersString))
                }

                // invokes the target method
                if (isStatic) {
                    invokestatic(targetClass.name, targetMethod.name, targetMethod.desc)
                } else {
                    invokevirtual(targetClass.name, targetMethod.name, targetMethod.desc)
                }
            }

            val returnInstructions = setOf(
                Opcodes.IRETURN,
                Opcodes.LRETURN,
                Opcodes.FRETURN,
                Opcodes.DRETURN,
                Opcodes.ARETURN,
            )
            var returnInstruction: AbstractInsnNode? = null
            for (r in returnInstructions) {
                val mixinReturnInstruction = mixinMethod.instructions.find { it.opcode == r }
                if (mixinReturnInstruction != null) {
                    returnInstruction = mixinReturnInstruction
                    break
                }
            }

            val generatedMethod = MethodNode(
                if (isStatic) Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC else Opcodes.ACC_PUBLIC,
                "${targetMethod.name}_weaveMixin\$ModifyArg",
                mixinMethod.desc,
                null,
                null
            ).apply {
                instructions = loads.apply {
                    if (invokedMethod.opcode != Opcodes.INVOKESTATIC) {
                        insert(VarInsnNode(Opcodes.ALOAD, 0))
                    }
                    add(MethodInsnNode(invokedMethod.opcode, invokedMethod.owner, invokedMethod.name, invokedMethod.desc, invokedMethod.itf))
                    add(returnInstruction)
                }
                targetClass.methods.add(this)
            }

            return listOf(targetMethod, generatedMethod)
        }

        private fun applyModifyConstant(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: ModifyConstant
        ): MethodNode {
            val method = targetClass.methods.find { "${it.name}${it.desc}" == annotation.method }
                ?: error("Failed to load Mixin ${mixinClass.name}: ${annotation.method} is not present in class ${targetClass.name}")
            val instructions = method.instructions
            val constant = annotation.constant
            val shift = constant.shift

            val generatedMethod = copyMixinToTarget(targetClass, mixinMethod)

            val targetInstruction: AbstractInsnNode = when(constant.constantType) {
                ConstantType.NULL -> {
                    instructions.findConstantInstruction<InsnNode>(
                        opcodes = intArrayOf(Opcodes.ACONST_NULL),
                        shift = shift
                    )
                }

                ConstantType.INT -> {
                    instructions.findConstantInstruction<AbstractInsnNode>(
                        opcodes =  intArrayOf(Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5, Opcodes.BIPUSH, Opcodes.SIPUSH, Opcodes.LDC),
                        insnConstant = { instruction ->
                            when (instruction.opcode) {
                                Opcodes.ICONST_M1 -> -1
                                Opcodes.ICONST_0 -> 0
                                Opcodes.ICONST_1 -> 1
                                Opcodes.ICONST_2 -> 2
                                Opcodes.ICONST_3 -> 3
                                Opcodes.ICONST_4 -> 4
                                Opcodes.ICONST_5 -> 5
                                Opcodes.BIPUSH -> (instruction as IntInsnNode).operand
                                Opcodes.SIPUSH -> (instruction as IntInsnNode).operand
                                Opcodes.LDC -> (instruction as LdcInsnNode).cst
                                else -> error("Failed to find instruction for ${instruction.opcode} (targetClass: ${targetClass.name}, method: ${method.name}, annotation: ${annotation.id})")
                            }
                        },
                        constant = constant.valueInt,
                        shift = shift
                    )
                }

                ConstantType.FLOAT -> {
                    instructions.findConstantInstruction<AbstractInsnNode>(
                        opcodes = intArrayOf(Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2, Opcodes.LDC),
                        insnConstant = { instruction ->
                            when (instruction.opcode) {
                                Opcodes.FCONST_0 -> 0.0f
                                Opcodes.FCONST_1 -> 1.0f
                                Opcodes.FCONST_2 -> 2.0f
                                Opcodes.LDC -> (instruction as LdcInsnNode).cst
                                else -> error("Failed to find instruction for ${instruction.opcode} (targetClass: ${targetClass.name}, method: ${method.name}, annotation: ${annotation.id})")
                            }
                        },
                        constant = constant.valueFloat,
                        shift = shift
                    )
                }

                ConstantType.LONG -> {
                    instructions.findConstantInstruction<AbstractInsnNode>(
                        opcodes = intArrayOf(Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.LDC),
                        insnConstant = { instruction ->
                            when (instruction.opcode) {
                                Opcodes.LCONST_0 -> 0L
                                Opcodes.LCONST_1 -> 1L
                                Opcodes.LDC -> (instruction as LdcInsnNode).cst
                                else -> error("Failed to find instruction for ${instruction.opcode} (targetClass: ${targetClass.name}, method: ${method.name}, annotation: ${annotation.id})")
                            }
                        },
                        constant = constant.valueLong,
                        shift = shift
                    )
                }

                ConstantType.DOUBLE -> {
                    println("Finding double constant")
                    instructions.findConstantInstruction<AbstractInsnNode>(
                        opcodes = intArrayOf(Opcodes.DCONST_0, Opcodes.DCONST_1, Opcodes.LDC),
                        insnConstant = { instruction ->
                            println("Instruction: $instruction")
                            if (instruction is LdcInsnNode) {
                                println("Instruction.cst: ${instruction.cst}")
                            }
                            when (instruction.opcode) {
                                Opcodes.DCONST_0 -> 0.0
                                Opcodes.DCONST_1 -> 1.0
                                Opcodes.LDC -> (instruction as LdcInsnNode).cst
                                else -> error("Failed to find instruction for ${instruction.opcode} (targetClass: ${targetClass.name}, method: ${method.name}, annotation: ${annotation.id})")
                            }
                        },
                        constant = constant.valueDouble.also { println("valueDouble: $it") },
                        shift = shift
                    )
                }

                ConstantType.STRING -> {
                    instructions.findConstantInstruction<AbstractInsnNode>(
                        opcodes = intArrayOf(Opcodes.LDC),
                        insnConstant = { instruction ->
                            when (instruction.opcode) {
                                Opcodes.LDC -> (instruction as LdcInsnNode).cst
                                else -> error("Failed to find instruction for ${instruction.opcode} (targetClass: ${targetClass.name}, method: ${method.name}, annotation: ${annotation.id})")
                            }
                        },
                        constant = constant.valueString,
                        shift = shift
                    )
                }

                ConstantType.CLASS -> {
                    instructions.findConstantInstruction<AbstractInsnNode>(
                        opcodes = intArrayOf(Opcodes.LDC),
                        insnConstant = { instruction ->
                            when (instruction.opcode) {
                                Opcodes.LDC -> (instruction as LdcInsnNode).cst
                                else -> error("Failed to find instruction for ${instruction.opcode} (targetClass: ${targetClass.name}, method: ${method.name}, annotation: ${annotation.id})")
                            }
                        },
                        constant = constant.valueClass,
                        shift = shift
                    )
                }
            }
                ?: error("Failed to find target instruction for ${constant.constantType} (targetClass: ${targetClass.name}, method: ${method.name}, annotation: ${annotation.id})")

            println("Target instruction: $targetInstruction")
            println("targetClass: ${targetClass.name}, generatedMethod: ${generatedMethod.name}, generatedMethod.desc: ${generatedMethod.desc}")

            instructions.insertBefore(targetInstruction, asm { aload(0)
//                f_same()
            })
            instructions.insert(targetInstruction, asm { invokevirtual(targetClass.name, generatedMethod.name, generatedMethod.desc)
//                f_same()
            })

            targetClass.dump("/tmp/targetClass.class")

            return generatedMethod
        }

        private fun applyAccessor(
            targetClass: ClassNode,
            mixinClass: Class<*>,
            mixinMethod: MethodNode,
            annotation: Accessor
        ): MethodNode {
            targetClass.interfaces.add(Type.getInternalName(mixinClass))

            val target = annotation.target
            val accessedField = targetClass.fields.find { it.name == target }
                ?: error("Failed to find $target field in ${targetClass.name}")

            val argTypes = Type.getArgumentTypes(mixinMethod.desc)
            if (argTypes.isNotEmpty()) {
                // setter
                if ((accessedField.access and Opcodes.ACC_FINAL) != 0)
                    error("Cannot create Accessor setter for a final field: $target in ${targetClass.name}")

                if (argTypes.size > 1)
                    error("Accessor setters should only have a single parameter")

                return MethodNode(
                    Opcodes.ACC_PUBLIC,
                    mixinMethod.name,
                    mixinMethod.desc,
                    "",
                    mixinMethod.exceptions.toTypedArray()
                ).also {
                    it.instructions = asm {
                        aload(0)
                        aload(1)
                        putfield(
                            targetClass.name,
                            accessedField.name,
                            accessedField.desc
                        )
                    }
                    targetClass.methods.add(it)
                }
            } else {
                val returnType = Type.getReturnType(mixinMethod.desc)
                if (returnType == Type.VOID_TYPE)
                    error("Cannot create Accessor getter with a void method: ${mixinMethod.name} for $target in ${targetClass.name}")

                // getter
                return MethodNode(
                    Opcodes.ACC_PUBLIC,
                    mixinMethod.name,
                    mixinMethod.desc,
                    "",
                    mixinMethod.exceptions.toTypedArray()
                ).also {
                    it.instructions = asm {
                        aload(0)
                        getfield(
                            targetClass.name,
                            accessedField.name,
                            accessedField.desc
                        )
                        +InsnNode(returnType.getOpcode(Opcodes.IRETURN))
                    }
                    targetClass.methods.add(it)
                }
            }
        }

        private fun applyInvoker(
            targetClass: ClassNode,
            mixinClass: Class<*>,
            mixinMethod: MethodNode
        ): MethodNode {
            return targetClass.methods
                .find { it.desc == mixinMethod.desc }
                .also { targetClass.interfaces.add(Type.getInternalName(mixinClass)) }
                ?: error("Failed to find method with matching descriptor: ${mixinMethod.desc} in ${targetClass.name}")
        }

        private fun applyRedirect(
            targetClass: ClassNode,
            mixinMethod: MethodNode,
            annotation: Redirect
        ): MethodNode {
            return mixinMethod
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
                if (method.access and Opcodes.ACC_STATIC != 0) Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC else Opcodes.ACC_PUBLIC,
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

inline fun <reified T : AbstractInsnNode> InsnList.findConstantInstruction(opcodes: IntArray, noinline insnConstant: ((T) -> Any)? = null, constant: Any? = null, shift: Int): T? {
    return filterIsInstance<T>()
        .filter { it.opcode in opcodes }
        .filter { insnConstant?.invoke(it) == constant }
        .elementAtOrNull(shift)
}


val MethodNode.parametersStrings: List<String>?
    get() = desc?.let { string -> Type.getArgumentTypes(string).map { it.descriptor } }

/**
 * Loads a local variable from the stack based on the [type descriptor][typeDescriptor].
 *
 * @return The size of the local variable on the stack.
 */
fun InsnBuilder.load(index: Int, typeDescriptor: String) =
    when (typeDescriptor) {
        "Z", "B", "C", "S", "I" -> {
            iload(index)
            1
        }
        "J" -> {
            lload(index)
            2
        }
        "F" -> {
            fload(index)
            1
        }
        "D" -> {
            dload(index)
            2
        }
        else -> {
            aload(index)
            1
        }
    }

/**
 * Pushes a value onto the stack based on the [value]'s type.
 *
 * @param value The value to push onto the stack.
 */
fun InsnBuilder.value(value: Any?) =
    when (value) {
        null -> aconst_null

        is Boolean -> if (value) iconst_1 else iconst_0

        is Byte -> bipush(value.toInt())

        is Short -> sipush(value.toInt())

        is Int -> when (value) {
            -1 -> iconst_m1
            0 -> iconst_0
            1 -> iconst_1
            2 -> iconst_2
            3 -> iconst_3
            4 -> iconst_4
            5 -> iconst_5
            else -> when {
                value >= -128 && value <= 127 -> bipush(value)
                value >= -32768 && value <= 32767 -> sipush(value)
                else -> ldc(value)
            }
        }

        is Long -> when (value) {
            0L -> lconst_0
            1L -> lconst_1
            else -> ldc(value)
        }

        is Float -> when (value) {
            0F -> fconst_0
            1F -> fconst_1
            2F -> fconst_2
            else -> ldc(value)
        }

        is Double -> when (value) {
            0.0 -> dconst_0
            1.0 -> dconst_1
            else -> ldc(value)
        }

        is String -> ldc(value)

        else -> throw IllegalArgumentException("Unsupported value type: ${value::class.java}")
    }

fun InsnBuilder.box(type: Type) {
    when (type) {
        Type.BOOLEAN_TYPE -> invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;")
        Type.BYTE_TYPE -> invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;")
        Type.SHORT_TYPE -> invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;")
        Type.INT_TYPE -> invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
        Type.LONG_TYPE -> invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;")
        Type.FLOAT_TYPE -> invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;")
        Type.DOUBLE_TYPE -> invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;")
        else -> throw IllegalArgumentException("Unsupported type: $type")
    }
}

fun InsnBuilder.boxNoException(type: Type) = runCatching { box(type) }

fun InsnBuilder.unbox(type: Type) {
    when (type) {
        Type.BOOLEAN_TYPE -> invokevirtual("java/lang/Boolean", "booleanValue", "()Z")
        Type.BYTE_TYPE -> invokevirtual("java/lang/Byte", "byteValue", "()B")
        Type.SHORT_TYPE -> invokevirtual("java/lang/Short", "shortValue", "()S")
        Type.INT_TYPE -> invokevirtual("java/lang/Integer", "intValue", "()I")
        Type.LONG_TYPE -> invokevirtual("java/lang/Long", "longValue", "()J")
        Type.FLOAT_TYPE -> invokevirtual("java/lang/Float", "floatValue", "()F")
        Type.DOUBLE_TYPE -> invokevirtual("java/lang/Double", "doubleValue", "()D")
        else -> throw IllegalArgumentException("Unsupported type: $type")
    }
}

fun InsnBuilder.unboxNoException(type: Type) = runCatching { unbox(type) }
