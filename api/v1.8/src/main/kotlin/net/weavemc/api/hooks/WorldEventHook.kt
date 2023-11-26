@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.api.event.WorldEvent
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * Corresponds to [WorldEvent.Load] and [WorldEvent.Unload].
 */
internal class WorldEventHook: Hook("net/minecraft/client/Minecraft") {

    /**
     * Inserts a call in [net.minecraft.client.Minecraft.loadWorld] to [WorldEvent.Load] and later [WorldEvent.Unload].
     *
     * @see net.minecraft.client.Minecraft.loadWorld
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {

        node.methods.named("loadWorld").instructions.insert(asm {
            val lbl = LabelNode()

            aload(0)
            getfield(
                "net/minecraft/client/Minecraft",
                "theWorld",
                "Lnet/minecraft/client/multiplayer/WorldClient;"
            )
            ifnull(lbl)

            new(internalNameOf<WorldEvent.Unload>())
            dup
            aload(0)
            getfield(
                "net/minecraft/client/Minecraft",
                "theWorld",
                "Lnet/minecraft/client/multiplayer/WorldClient;"
            )
            invokespecial(
                internalNameOf<WorldEvent.Unload>(),
                "<init>",
                "(Lnet/minecraft/world/World;)V"
            )
            callEvent()

            +lbl
            f_same()

            val end = LabelNode()
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
            callEvent()

            +end
            f_same()
        })
    }
}
