package net.weavemc.loader.bootstrap.transformer

import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.LdcInsnNode

// Finalizes mixin relocation
object MixinRelocator : SafeTransformer {
    override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
        if (!className.startsWith("net/weavemc/relocate/spongepowered")) return null

        val reader = originalClass.asClassReader()
        val node = reader.asClassNode()

        fun String.relocate() = replace("org.spongepowered", "net.weavemc.relocate.spongepowered")

        node.fields.forEach {
            val v = it.value
            if (v is String) it.value = v.relocate()
        }

        node.methods.forEach { m ->
            m.instructions.filterIsInstance<LdcInsnNode>().forEach {
                val v = it.cst
                if (v is String) it.cst = v.relocate()
            }
        }

        val writer = ClassWriter(reader, 0)
        node.accept(writer)
        return writer.toByteArray()
    }
}