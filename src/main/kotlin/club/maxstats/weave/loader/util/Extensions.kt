package club.maxstats.weave.loader.util

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Method
import kotlin.reflect.KFunction

fun MethodVisitor.loadConstant(value: Any?) = when (value) {

    null -> visitInsn(ACONST_NULL)
    true -> visitInsn(ICONST_1)
    false -> visitInsn(ICONST_0)
    is Byte -> {
        visitIntInsn(BIPUSH, value.toInt())
        visitInsn(I2B)
    }

    is Int -> when (value) {
        -1 -> visitInsn(ICONST_M1)
        0 -> visitInsn(ICONST_0)
        1 -> visitInsn(ICONST_1)
        2 -> visitInsn(ICONST_2)
        3 -> visitInsn(ICONST_3)
        4 -> visitInsn(ICONST_4)
        5 -> visitInsn(ICONST_5)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> visitIntInsn(BIPUSH, value)
        in Short.MIN_VALUE..Short.MAX_VALUE -> visitIntInsn(SIPUSH, value)
        else -> visitLdcInsn(value)
    }

    is Float -> when (value) {
        0f -> visitInsn(FCONST_0)
        1f -> visitInsn(FCONST_1)
        2f -> visitInsn(FCONST_2)
        else -> visitLdcInsn(value)
    }

    is Double -> when (value) {
        0.0 -> visitInsn(DCONST_0)
        1.0 -> visitInsn(DCONST_1)
        else -> visitLdcInsn(value)
    }

    is Long -> when (value) {
        0L -> visitInsn(LCONST_0)
        1L -> visitInsn(LCONST_1)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> {
            visitIntInsn(BIPUSH, value.toInt())
            visitInsn(I2L)
        }

        in Short.MIN_VALUE..Short.MAX_VALUE -> {
            visitIntInsn(SIPUSH, value.toInt())
            visitInsn(I2L)
        }

        else -> visitLdcInsn(value)
    }

    is Char -> {
        visitIntInsn(BIPUSH, value.code)
        visitInsn(I2C)
    }

    is Short -> {
        visitIntInsn(SIPUSH, value.toInt())
        visitInsn(I2S)
    }

    is String, is Type, is Handle, is ConstantDynamic -> visitLdcInsn(value)
    else -> error("Constant value ($value) is not a valid JVM constant!")
}

fun List<MethodNode>.named(name: String) = first { it.name == name }
fun List<FieldNode>.named(name: String) = first { it.name == name }

fun List<MethodNode>.similar(method: Method) =
    find { it.name == method.name && it.desc == Type.getMethodDescriptor(method) }

fun List<MethodNode>.similar(func: KFunction<*>) = similar(func.java)

inline fun <reified T : Any> internalNameOf() = T::class.java.name.replace('.', '/')

val Method.isStatic get() = modifiers and ACC_STATIC != 0
val Method.isPrivate get() = modifiers and ACC_PRIVATE != 0
val Method.isFinal get() = modifiers and ACC_FINAL != 0
val Method.isProtected get() = modifiers and ACC_PROTECTED != 0
val Method.isPublic get() = modifiers and ACC_PUBLIC != 0
val Method.isAbstract get() = modifiers and ACC_ABSTRACT != 0

val MethodNode.isStatic get() = access and ACC_STATIC != 0
val MethodNode.isPrivate get() = access and ACC_PRIVATE != 0
val MethodNode.isFinal get() = access and ACC_FINAL != 0
val MethodNode.isProtected get() = access and ACC_PROTECTED != 0
val MethodNode.isPublic get() = access and ACC_PUBLIC != 0
val MethodNode.isAbstract get() = access and ACC_ABSTRACT != 0

fun ClassNode.generateMethod(
    name: String = generateMethodName(),
    desc: String = "()V",
    access: Int = ACC_PUBLIC,
    insns: InsnBuilder.() -> Unit = {}
) = MethodNode(access, name, desc, null, null).apply { instructions = asm(insns) }