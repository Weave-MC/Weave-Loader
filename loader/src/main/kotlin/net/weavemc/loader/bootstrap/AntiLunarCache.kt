package net.weavemc.loader.bootstrap

import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.named
import net.weavemc.api.bytecode.next
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

object AntiLunarCache : SafeTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        originalClass: ByteArray
    ): ByteArray? {
        if (className != "com/moonsworth/lunar/genesis/Genesis") return null

        val reader = ClassReader(originalClass)
        val node = ClassNode().also { reader.accept(it, 0) }

        with(node.methods.named("prebake")) {
            instructions = asm { _return }
            tryCatchBlocks.clear()
            exceptions.clear()
            localVariables.clear()
        }

        val mainMethod = node.methods.named("main")
        val cstNode = mainMethod.instructions.first { it is LdcInsnNode && it.cst == "prebake.cache" }
        val branch = cstNode.next<MethodInsnNode> { it.name == "exists" } ?: error("Lunar binary incompatible")

        mainMethod.instructions.insert(branch, asm {
            pop
            pop
            iconst_0
        })

        mainMethod.instructions.remove(branch)
        return ClassWriter(reader, ClassWriter.COMPUTE_MAXS).also { node.accept(it) }.toByteArray()
    }
}