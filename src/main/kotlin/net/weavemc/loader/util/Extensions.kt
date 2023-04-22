package net.weavemc.loader.util

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.io.File

internal fun List<MethodNode>.named(name: String) = find { it.name == name }!!
internal fun List<FieldNode>.named(name: String) = find { it.name == name }!!

internal inline fun <reified T : Any> internalNameOf(): String = Type.getInternalName(T::class.java)

internal inline fun <reified T : AbstractInsnNode> AbstractInsnNode.next(p: (T) -> Boolean = { true }): T? {
    var insn = this

    while (true) {
        insn = insn.next ?: return null
        if (insn is T && p(insn)) return insn
    }
}

internal inline fun <reified T : AbstractInsnNode> AbstractInsnNode.prev(p: (T) -> Boolean = { true }): T? {
    var insn = this

    while (true) {
        insn = insn.previous ?: return null
        if (insn is T && p(insn)) return insn
    }
}

internal fun InsnList.insertBeforeReturn(insnList: InsnList) {
    val returnNode = findLast { it.opcode == Opcodes.RETURN } ?: return
    insertBefore(returnNode, insnList)
}

internal fun ClassNode.dump(file: String) {
    val cw = ClassWriter(0)
    accept(cw)
    File(file).writeBytes(cw.toByteArray())
}
