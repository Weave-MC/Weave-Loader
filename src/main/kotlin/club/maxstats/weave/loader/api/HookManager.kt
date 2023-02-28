package club.maxstats.weave.loader.api

import club.maxstats.weave.loader.hooks.impl.InputEventHook
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import java.util.function.BiConsumer
import java.util.function.Consumer

@Suppress("MemberVisibilityCanBePrivate")
class HookManager {
    private val hooks = mutableListOf<Hook>(
        InputEventHook()
    )

    fun add(vararg hooks: Hook) {
        this.hooks += hooks
    }

    fun register(name: String, f: BiConsumer<ClassNode, Hook.AssemblerConfig>) {
        hooks += object : Hook(name) {
            override fun transform(cn: ClassNode, conf: AssemblerConfig) {
                f.accept(cn, conf)
            }
        }
    }

    fun register(name: String, f: Consumer<ClassNode>) = register(name) { cn, _ ->
        f.accept(cn)
    }

    internal inner class Transformer : ClassFileTransformer {
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
            val conf = object : Hook.AssemblerConfig() {
                override fun computeFrames() {
                    computeFrames = true
                }
            }
            hooks.forEach { it.transform(cn, conf) }

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