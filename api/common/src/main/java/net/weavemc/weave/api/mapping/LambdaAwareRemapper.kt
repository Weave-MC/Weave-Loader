package net.weavemc.weave.api.mapping

import org.objectweb.asm.*
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper

class LambdaAwareRemapper(
    parent: ClassVisitor,
    remapper: Remapper
) : ClassRemapper(Opcodes.ASM9, parent, remapper) {
    override fun createMethodRemapper(parent: MethodVisitor) = object : MethodRemapper(Opcodes.ASM9, parent, remapper) {
        override fun visitInvokeDynamicInsn(name: String, descriptor: String, handle: Handle, vararg args: Any) {
            val remappedName = if (
                handle.owner == "java/lang/invoke/LambdaMetafactory" &&
                (handle.name == "metafactory" || handle.name == "altMetafactory")
            ) {
                // Lambda, so we need to rename it... weird edge case, maybe ASM issue?
                // LambdaMetafactory just causes an IncompatibleClassChangeError if the lambda is invalid
                // Does it assume correct compile time? odd.
                remapper.mapMethodName(
                    Type.getReturnType(descriptor).internalName,
                    name,
                    (args.first() as Type).descriptor
                )
            } else name

            parent.visitInvokeDynamicInsn(
                /* name = */ remappedName,
                /* descriptor = */ remapper.mapMethodDesc(descriptor),
                /* bootstrapMethodHandle = */ remapper.mapValue(handle) as Handle,
                /* ...bootstrapMethodArguments = */ *args.map { remapper.mapValue(it) }.toTypedArray()
            )
        }
    }
}
