package net.weavemc.loader

import net.weavemc.loader.api.Hook
import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.loader.hooks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.transformers.MixinClassWriter

internal object HookManager : SafeTransformer {
    val hooks = mutableListOf(
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
        ServerConnectEventHook(),
        ShutdownEventHook(),
        StartGameEventHook(),
        TickEventHook(),
        WorldEventHook(),
        PacketEventHook()
    )

    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        val hooks = hooks.filter { it.targets.isEmpty() || it.targets.contains(className) }
        if (hooks.isEmpty()) return null

        val node = ClassNode()
        val reader = ClassReader(originalClass)
        reader.accept(node, 0)

        var computeFrames = false
        val cfg = Hook.AssemblerConfig { computeFrames = true }

        hooks.forEach { it.transform(node, cfg) }
        val flags = if (computeFrames) ClassWriter.COMPUTE_FRAMES else ClassWriter.COMPUTE_MAXS

        // HACK: use MixinClassWriter because it doesn't load classes when computing frames.
        val writer = MixinClassWriter(reader, flags)
        node.accept(writer)
        return writer.toByteArray()
    }
}
