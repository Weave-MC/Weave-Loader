package club.maxstats.weave.loader.util

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.lang.reflect.Field
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

val Field.isStatic get() = modifiers and ACC_STATIC != 0
val Field.isPrivate get() = modifiers and ACC_PRIVATE != 0
val Field.isFinal get() = modifiers and ACC_FINAL != 0
val Field.isProtected get() = modifiers and ACC_PROTECTED != 0
val Field.isPublic get() = modifiers and ACC_PUBLIC != 0

val MethodNode.isStatic get() = access and ACC_STATIC != 0
val MethodNode.isPrivate get() = access and ACC_PRIVATE != 0
val MethodNode.isFinal get() = access and ACC_FINAL != 0
val MethodNode.isProtected get() = access and ACC_PROTECTED != 0
val MethodNode.isPublic get() = access and ACC_PUBLIC != 0
val MethodNode.isAbstract get() = access and ACC_ABSTRACT != 0

val FieldNode.isStatic get() = access and ACC_STATIC != 0
val FieldNode.isPrivate get() = access and ACC_PRIVATE != 0
val FieldNode.isFinal get() = access and ACC_FINAL != 0
val FieldNode.isProtected get() = access and ACC_PROTECTED != 0
val FieldNode.isPublic get() = access and ACC_PUBLIC != 0

fun ClassNode.generateMethod(
    name: String = generateMethodName(),
    desc: String,
    access: Int = ACC_PUBLIC,
    add: Boolean = true,
    insns: InsnBuilder.() -> Unit = {}
) = MethodNode(access, name, desc, null, null).also {
    it.visitAsm(insns)

    if (add) {
        methods.add(it)
    }
}

inline fun <reified T : AbstractInsnNode> AbstractInsnNode.next(p: (T) -> Boolean = { true }): T? {
    var insn = this

    while (true){
        insn = insn.next ?: return null
        if(insn is T && p(insn)) return insn
    }
}

inline fun <reified T : AbstractInsnNode> AbstractInsnNode.prev(p: (T) -> Boolean = { true }): T? {
    var insn = this

    while (true){
        insn = insn.previous ?: return null
        if(insn is T && p(insn)) return insn
    }
}

fun ClassNode.dump(file: String) {
    val cw = ClassWriter(0)
    accept(cw)
    File(file).writeBytes(cw.toByteArray())
}
