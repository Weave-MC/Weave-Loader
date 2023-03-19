package club.maxstats.weave.loader.api

import club.maxstats.weave.loader.bootstrap.SafeTransformer
import club.maxstats.weave.loader.hooks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import java.util.function.Consumer
import kotlin.reflect.KClass

class HookManager {

    private val hooks = mutableListOf(
        ChatReceivedEventHook(),
        ChatSentEventHook(),
        EntityListEventAddHook(), EntityListEventRemoveHook(),
        GuiOpenEventHook(),
        KeyboardEventHook(),
        MouseEventHook(),
        PlayerListEventHook(),
        RenderGameOverlayHook(),
        RenderHandEventHook(),
        RenderLivingEventHook(),
        RenderWorldEventHook(),
        ShutdownEventHook(),
        StartGameEventHook(),
        TickEventHook(),
    )

    fun register(vararg hooks: Hook) {
        this.hooks += hooks
    }

    fun register(name: String, block: Consumer<ClassNode>) = hooks.add(
        object : Hook(name) {
            override fun transform(node: ClassNode, cfg: AssemblerConfig) {
                block.accept(node)
            }
        }
    )

    fun register(clazz: Class<*>, block: Consumer<ClassNode>) =
        register(Type.getInternalName(clazz), block)

    fun register(clazz: KClass<*>, block: Consumer<ClassNode>) =
        register(clazz.java, block)

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

            var computeFrames = false
            val cfg = object : Hook.AssemblerConfig() {
                override fun computeFrames() {
                    computeFrames = true
                }
            }

            hooks.forEach { it.transform(node, cfg) }
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
