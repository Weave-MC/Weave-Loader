package club.maxstats.weave.loader.hooks

import club.maxstats.weave.api.hook.Hook
import club.maxstats.weave.api.hook.HookManager
import club.maxstats.weave.loader.hooks.impl.InitHook
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class HookManagerImpl : HookManager {
    private val hooks = mutableListOf<Hook>(
        InitHook()
    )

    override fun add(hook: Hook) {
        hooks += hook
    }

    inner class Transformer : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            originalClass: ByteArray
        ): ByteArray? {
            val hooks = hooks.filter { it.className == className }
            if (hooks.isEmpty()) return null

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
}