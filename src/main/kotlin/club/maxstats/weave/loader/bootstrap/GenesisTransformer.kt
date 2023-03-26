package club.maxstats.weave.loader.bootstrap

import club.maxstats.weave.loader.api.util.asm
import club.maxstats.weave.loader.util.named
import club.maxstats.weave.loader.util.next
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

internal object GenesisTransformer : SafeTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        originalClass: ByteArray
    ) = if (className == "com/moonsworth/lunar/genesis/Genesis") originalClass.modify { node ->
        val method = node.methods.named("main")
        val ldc = method.instructions.first { it is LdcInsnNode && it.cst == "prebake.cache" }

        method.instructions.insert(
            ldc.next<MethodInsnNode> { it.name == "exists" },
            asm {
                pop
                iconst_0
            }
        )

        val prebake = node.methods.named("prebake")
        prebake.instructions = asm { _return }
        prebake.exceptions.clear()
        prebake.tryCatchBlocks.clear()
        prebake.localVariables.clear()
    } else null
}

private fun ByteArray.modify(handler: (node: ClassNode) -> Unit): ByteArray {
    val reader = ClassReader(this)
    val node = ClassNode().also { reader.accept(it, 0) }.also(handler)
    val writer = ClassWriter(reader, 0)
    node.accept(writer)
    return writer.toByteArray()
}
