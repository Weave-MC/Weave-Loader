package net.weavemc.loader.bootstrap.transformer

import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.mixin.LoaderClassWriter
import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.fatalError
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * Transformer meant to start the initialization phase for Weave Mods by hooking [net.minecraft.client.main.Main.main].
 *
 * @see [net.weavemc.api.ModInitializer.init]
 */
internal object ModInitializerHook : SafeTransformer {
    override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
        if (className != "net/minecraft/client/main/Main") return null

        val reader = ClassReader(originalClass)
        val node = reader.asClassNode()

        val main = node.methods.find { it.name == "main" } ?: fatalError("Failed to find main method in $className")
        main.instructions.insert(asm {
            invokestatic(internalNameOf<WeaveLoader>(), "getInstance", "()L${internalNameOf<WeaveLoader>()};")
            invokevirtual(internalNameOf<WeaveLoader>(), "initializeMods", "()V")
        })

        return LoaderClassWriter(loader, reader, ClassWriter.COMPUTE_MAXS).also { node.accept(it) }.toByteArray()
    }
}