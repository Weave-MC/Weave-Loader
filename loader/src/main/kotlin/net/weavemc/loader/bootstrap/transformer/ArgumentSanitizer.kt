package net.weavemc.loader.bootstrap.transformer

import net.weavemc.internals.asm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

object ArgumentSanitizer : SafeTransformer {
    override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
        if (className != "sun/management/RuntimeImpl") return null

        val node = ClassNode().also { ClassReader(originalClass).accept(it, 0) }
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        with(node.methods.first { it.name == "getInputArguments" }) {
            val insn = instructions.first { it.opcode == Opcodes.ARETURN }
            instructions.insertBefore(insn, asm {
                invokeinterface("java/lang/Iterable", "iterator", "()Ljava/util/Iterator;")
                astore(2)
                new("java/util/ArrayList")
                dup
                invokespecial("java/util/ArrayList", "<init>", "()V")
                astore(3)

                val loop = LabelNode()
                val end = LabelNode()

                +loop
                aload(2)
                invokeinterface("java/util/Iterator", "hasNext", "()Z")
                ifeq(end)

                aload(2)
                invokeinterface("java/util/Iterator", "next", "()Ljava/lang/Object;")
                checkcast("java/lang/String")
                dup
                astore(4)
                ldc("javaagent")
                invokevirtual("java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z")
                ifne(loop)

                aload(3)
                aload(4)
                invokeinterface("java/util/List", "add", "(Ljava/lang/Object;)Z")
                pop

                goto(loop)
                +end

                aload(3)
            })
        }

        node.accept(writer)

        return writer.toByteArray()
    }
}