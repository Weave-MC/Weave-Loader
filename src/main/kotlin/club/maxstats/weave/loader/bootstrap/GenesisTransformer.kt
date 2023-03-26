package club.maxstats.weave.loader.bootstrap

import club.maxstats.weave.loader.util.named
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

internal object GenesisTransformer : SafeTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        originalClass: ByteArray
    ): ByteArray? {
        if (className != "com/moonsworth/lunar/genesis/Genesis") return null

        val cn = ClassNode()
        val cr = ClassReader(originalClass)
        cr.accept(cn, 0)

        /*
        * replace noClassCache with a flag that always gets passed
        * done this way because it'll throw if/when lunar removes --noClassCache
        * */

        cn.methods.named("main").instructions
            .filterIsInstance<LdcInsnNode>()
            .first { it.cst == "noClassCache" }
            .cst = "gameDir"

        val cw = ClassWriter(cr, 0)
        cn.accept(cw)

        return cw.toByteArray()
    }
}
