package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.event.impl.InputEvent
import club.maxstats.weave.loader.hooks.impl.InputEventHook
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class HookManagerImpl : HookManager {
    private val hooks = mutableListOf<Hook>(
        InputEventHook()
    )

    override fun add(vararg hooks: Hook) {
        this.hooks += hooks
    }

    inner class Transformer : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            originalClass: ByteArray
        ): ByteArray? {
            val hooks = hooks.filter { it.targetClassName == className }
            if (hooks.isEmpty()) return null

            val cn = ClassNode()
            val cr = ClassReader(originalClass)
            cr.accept(cn, 0)

            var computeFrames = false
            val callback = object : Hook.Callback() {
                override fun computeFrames() {
                    computeFrames = true
                }
            }
            hooks.forEach { it.transform(cn, callback) }

            val cwFlags = if (computeFrames) {
                ClassWriter.COMPUTE_FRAMES
            } else {
                ClassWriter.COMPUTE_MAXS
            }

            val cw = object : ClassWriter(cr, cwFlags) {
                override fun getClassLoader() = loader
            }
            cn.accept(cw)
            return cw.toByteArray()
        }
    }
}