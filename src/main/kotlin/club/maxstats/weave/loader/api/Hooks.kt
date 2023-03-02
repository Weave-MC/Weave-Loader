package club.maxstats.weave.loader.api

import club.maxstats.weave.loader.asmAPILevel
import club.maxstats.weave.loader.hooks.registerDefaultHooks
import club.maxstats.weave.loader.util.loadConstant
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class HookManager {

    private val hooks = mutableListOf<HookInfo>()

    fun register(hook: JavaHook) {
        hooks += HookInfo(hook.targetName) { hook.transform(node).apply(this) }
    }

    fun register(name: String, block: HookContext.() -> Unit) {
        hooks += HookInfo(name, block)
    }

    init {
        registerDefaultHooks()
    }

    internal inner class Transformer : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            originalClass: ByteArray
        ): ByteArray? {
            val hooks = hooks.filter { it.targetName == className }
            if (hooks.isEmpty()) return null

            val node = ClassNode()
            val reader = ClassReader(originalClass)
            reader.accept(node, 0)

            val contexts = hooks.map { HookContext(node).also(it.hook) }
            val computeFrames = contexts.any { it.computeFrames }
            val flags = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS

            val writer = object : ClassWriter(reader, flags) {
                override fun getClassLoader() = loader
            }

            val foldedVisitors = contexts.flatMap { it.visitors }.fold(writer)
            node.accept(foldedVisitors)
            return writer.toByteArray()
        }
    }

}

abstract class JavaHook(val targetName: String) {

    abstract fun transform(node: ClassNode): AssemblerConfiguration

    data class AssemblerConfiguration(val computeFrames: Boolean = false) {
        internal fun apply(ctx: HookContext) {
            if (computeFrames) ctx.computeFrames()
        }
    }

}

data class HookInfo(val targetName: String, val hook: HookContext.() -> Unit)

typealias ClassVisitorWrapper  = (parent: ClassVisitor)  -> ClassVisitor
typealias MethodVisitorWrapper = (parent: MethodVisitor) -> MethodVisitor

fun List<ClassVisitorWrapper>.fold(parent: ClassVisitor)   = fold(parent) { acc, curr -> curr(acc) }
fun List<MethodVisitorWrapper>.fold(parent: MethodVisitor) = fold(parent) { acc, curr -> curr(acc) }

class HookContext(val node: ClassNode) {

    internal val visitors = mutableListOf<ClassVisitorWrapper>()

    internal var computeFrames = false
        private set

    fun computeFrames() {
        computeFrames = true
    }

    fun visitor(wrapper: ClassVisitorWrapper) {
        visitors += wrapper
    }

    fun methodVisitor(method: MethodNode, wrapper: MethodVisitorWrapper) = visitor { parent ->
        object : ClassVisitor(asmAPILevel, parent) {
            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<String>?
            ): MethodVisitor {
                val original = super.visitMethod(access, name, descriptor, signature, exceptions)
                return if (method.name == name && method.desc == descriptor) wrapper(original) else original
            }
        }
    }

    inline fun methodTransform(method: MethodNode, crossinline builder: MethodVisitorBuilder.() -> Unit) =
        methodVisitor(method) { parent -> MethodVisitorBuilder().apply(builder).build(parent) }

}

class MethodVisitorBuilder {

    private val visitors = mutableListOf<MethodVisitorWrapper>()

    fun overwrite(impl: MethodVisitor.() -> Unit) {
        visitors += { parent ->
            object : MethodVisitor(asmAPILevel, null) {
                override fun visitCode() {
                    parent.visitCode()
                    parent.impl()
                    parent.visitMaxs(0, 0)
                    parent.visitEnd()
                }

                override fun visitParameter(name: String, access: Int) {
                    parent.visitParameter(name, access)
                }
            }
        }
    }

    fun fixedValue(cst: Any?, returnType: Type) = overwrite {
        loadConstant(cst)
        visitInsn(returnType.getOpcode(IRETURN))
    }

    fun stubValue(returnType: Type) = overwrite {
        visitInsn(
            when (returnType.sort) {
                Type.VOID -> NOP
                Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.CHAR, Type.INT -> ICONST_0
                Type.FLOAT -> FCONST_0
                Type.DOUBLE -> DCONST_0
                Type.LONG -> LCONST_0
                Type.OBJECT, Type.ARRAY -> ACONST_NULL
                else -> error("Invalid non-value type")
            }
        )

        visitInsn(returnType.getOpcode(IRETURN))
    }

    fun visitor(wrapper: MethodVisitorWrapper) {
        visitors += { parent -> wrapper(parent) }
    }

    fun methodEnter(handler: MethodVisitor.() -> Unit) {
        visitors += { parent ->
            object : MethodVisitor(asmAPILevel, parent) {
                override fun visitCode() {
                    super.visitCode()
                    handler()
                }
            }
        }
    }

    fun methodExit(handler: MethodVisitor.(opcode: Int) -> Unit) {
        visitors += { parent ->
            object : MethodVisitor(asmAPILevel, parent) {
                override fun visitInsn(opcode: Int) {
                    if (opcode in IRETURN..RETURN || opcode == ATHROW) handler(opcode)
                    super.visitInsn(opcode)
                }
            }
        }
    }

    fun methodExit(opcode: Int, handler: MethodVisitor.() -> Unit) =
        methodExit { op -> if (op == opcode) handler() }

    fun callAdvice(
        matcher: (owner: String, name: String, desc: String) -> Boolean,
        beforeCall: MethodVisitor.() -> Unit = {},
        afterCall: MethodVisitor.() -> Unit = {},
        handleOnce: Boolean = false
    ) {
        visitors += { parent ->
            object : MethodVisitor(asmAPILevel, parent) {
                private var hasHandled = false

                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    name: String,
                    descriptor: String,
                    isInterface: Boolean
                ) {
                    if (hasHandled && handleOnce)
                        return super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

                    val matches = matcher(owner, name, descriptor)

                    if (matches) {
                        hasHandled = true
                        beforeCall()
                    }

                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    if (matches) afterCall()
                }
            }
        }
    }

    fun replaceCall(
        matcher: (owner: String, name: String, desc: String) -> Boolean,
        replacement: MethodVisitor.() -> Unit = {},
    ) {
        visitors += { parent ->
            object : MethodVisitor(asmAPILevel, parent) {
                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    name: String,
                    descriptor: String,
                    isInterface: Boolean
                ) {
                    if (matcher(owner, name, descriptor)) replacement()
                    else super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }
            }
        }
    }

    fun advice(
        enter: MethodVisitor.() -> Unit = {},
        exit: MethodVisitor.(opcode: Int) -> Unit = {}
    ) {
        methodEnter(enter)
        methodExit(exit)
    }

    fun replaceConstant(from: Any, to: Any) = replaceConstants(mapOf(from to to))

    fun replaceConstants(map: Map<Any, Any>) = visitor { parent ->
        object : MethodVisitor(asmAPILevel, parent) {
            override fun visitLdcInsn(value: Any) =
                super.visitLdcInsn(map[value] ?: value)

            override fun visitInvokeDynamicInsn(
                name: String,
                desc: String,
                handle: Handle,
                vararg bsmArgs: Any
            ) {
                val newArgs = bsmArgs.map { map[it] ?: it }.toTypedArray()
                super.visitInvokeDynamicInsn(name, desc, handle, *newArgs)
            }

            override fun visitIntInsn(opcode: Int, operand: Int) {
                parent.loadConstant(
                    if (opcode == BIPUSH || opcode == SIPUSH) map[operand] as? Int ?: operand else operand
                )
            }
        }
    }

    fun replaceString(from: String, to: String) = visitor { parent ->
        object : MethodVisitor(asmAPILevel, parent) {
            override fun visitLdcInsn(value: Any?) {
                super.visitLdcInsn(if (value is String) value.replace(from, to) else value)
            }

            override fun visitInvokeDynamicInsn(
                name: String,
                desc: String,
                handle: Handle,
                vararg bsmArgs: Any
            ) {
                val newArgs = bsmArgs.map { if (it is String) it.replace(from, to) else it }.toTypedArray()
                super.visitInvokeDynamicInsn(name, desc, handle, *newArgs)
            }
        }
    }

    fun build(parent: MethodVisitor) = visitors.fold(parent)

}