package net.weavemc.weave.api.mapping

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AccessWideningVisitor(parent: ClassVisitor) : ClassVisitor(Opcodes.ASM9, parent) {
    private fun Int.widen() = this and (Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED).inv() or Opcodes.ACC_PUBLIC
    private fun Int.removeFinal() = this and Opcodes.ACC_FINAL.inv()

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        super.visit(version, access.widen().removeFinal(), name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor = super.visitMethod(access.widen().removeFinal(), name, descriptor, signature, exceptions)

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor = super.visitField(access.widen(), name, descriptor, signature, value)
}
