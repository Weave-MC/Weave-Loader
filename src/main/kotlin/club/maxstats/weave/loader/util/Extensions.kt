package club.maxstats.weave.loader.util

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Method

fun List<MethodNode>.named(name: String) = find { it.name == name }!!
fun List<FieldNode>.named(name: String) = find { it.name == name }!!

inline fun <reified T : Any> internalNameOf(): String = Type.getInternalName(T::class.java)

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
    desc: String,
    access: Int = ACC_PUBLIC,
    insns: InsnBuilder.() -> Unit = {}
) = MethodNode(access, name, desc, null, null).also { it.visitAsm(insns) }