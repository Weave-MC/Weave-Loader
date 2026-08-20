package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.WorldEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.search
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * Corresponds to [WorldEvent.Load] and [WorldEvent.Unload].
 */
internal class WorldEventHook : Hook("net/minecraft/client/MinecraftClient") {
    /**
     * Inserts calls to [WorldEvent.Load] and [WorldEvent.Unload] using the Event Bus.
     *
     * @see net.minecraft.client.MinecraftClient.setWorld
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.search(
            "setWorld",
            "(Lnet/minecraft/client/world/ClientWorld;)V"
        ).instructions.insert(asm {
            val label = LabelNode()

            // if current world is non-null -> fire Unload
            aload(0)
            getfield(
                "net/minecraft/client/MinecraftClient",
                "world",
                "Lnet/minecraft/client/world/ClientWorld;"
            )
            ifnull(label)

            new(internalNameOf<WorldEvent.Unload>())
            dup
            aload(0)
            getfield(
                "net/minecraft/client/MinecraftClient",
                "world",
                "Lnet/minecraft/client/world/ClientWorld;"
            )
            invokespecial(
                internalNameOf<WorldEvent.Unload>(),
                "<init>",
                "(Lnet/minecraft/world/World;)V"
            )
            postEvent()

            +label
            f_same()

            val end = LabelNode()

            // if new incoming world is non-null -> fire Load
            aload(1)
            ifnull(end)

            new(internalNameOf<WorldEvent.Load>())
            dup
            aload(1)
            invokespecial(
                internalNameOf<WorldEvent.Load>(),
                "<init>",
                "(Lnet/minecraft/world/World;)V"
            )
            postEvent()

            +end
            f_same()
        })
    }
}