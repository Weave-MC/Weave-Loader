package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.hooks.impl.InitHook
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class HookTransformer : ClassFileTransformer {

    private val hooks = arrayOf<Hook>(
        InitHook()
    )

    override fun transform(
        loader: ClassLoader,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        originalClass: ByteArray
    ): ByteArray {
        val hooks = hooks.filter { it.name == className }
        if (hooks.isEmpty()) return originalClass

        val cn = ClassNode()
        val cr = ClassReader(originalClass)
        cr.accept(cn, 0)

        hooks.forEach { it.transform(cn) }
        val cwFlags = hooks.map { it.cwFlags }.reduce(Int::or)

        val cw = object : ClassWriter(cr, cwFlags) {
            override fun getClassLoader() = loader
        }

        cn.accept(cw)

        return cw.toByteArray()
    }

}