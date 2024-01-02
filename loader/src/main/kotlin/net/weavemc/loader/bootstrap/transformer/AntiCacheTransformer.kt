package net.weavemc.loader.bootstrap.transformer

import net.weavemc.internals.asm
import net.weavemc.internals.named
import net.weavemc.internals.next
import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

object AntiCacheTransformer : SafeTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        originalClass: ByteArray
    ): ByteArray? {
        if (className != "com/moonsworth/lunar/genesis/Genesis") return null

        val reader = originalClass.asClassReader()
        val node = reader.asClassNode()

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