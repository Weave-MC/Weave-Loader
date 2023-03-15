package club.maxstats.weave.loader.api

import club.maxstats.weave.loader.bootstrap.SafeTransformer
import club.maxstats.weave.loader.hooks.registerDefaultHooks
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.util.function.BiConsumer
import java.util.function.Consumer

class HookManager {

    private val hooks = mutableListOf<Hook>()

    init {
        registerDefaultHooks()
    }

    fun register(vararg hook: Hook) {
        hooks += hook
    }

    fun register(name: String, block: BiConsumer<ClassNode, Hook.AssemblerConfig>) {
        hooks += object : Hook(name) {
            override fun transform(node: ClassNode, cfg: AssemblerConfig) {
                block.accept(node, cfg)
            }
        }
    }

    fun register(name: String, block: HookContext.() -> Unit) =
        register(name) { node, cfg -> HookContext(node, cfg).block() }

    fun register(name: String, block: Consumer<ClassNode>) = register(name) { cn, _ -> block.accept(cn) }

    internal inner class Transformer : SafeTransformer {

        override fun transform(
            loader: ClassLoader,
            className: String,
            originalClass: ByteArray
        ): ByteArray? {
            val hooks = hooks.filter { it.targetClassName == className }
            if (hooks.isEmpty()) return null

            val node = ClassNode()
            val reader = ClassReader(originalClass)
            reader.accept(node, 0)

            val configs = hooks.map { hook ->
                Hook.AssemblerConfig().also { hook.transform(node, it) }
            }
            val computeFrames = configs.any { it.computeFrames }
            val flags = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS

            val writer = object : ClassWriter(reader, flags) {
                override fun getClassLoader() = loader
            }
            node.accept(writer)
            return writer.toByteArray()
        }
    }

}

class HookContext(val node: ClassNode, val config: Hook.AssemblerConfig)
