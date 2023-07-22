@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.world.World
import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
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
        node.methods.find {
            it.name == "loadWorld" && it.desc == "(L${internalNameOf<WorldClient>()};Ljava/lang/String;)V"
        }!!.instructions.insert(asm {
            val lbl = LabelNode()

            aload(0)
            getfield(
                internalNameOf<Minecraft>(),
                "theWorld",
                "L${internalNameOf<WorldClient>()};"
            )
            ifnull(lbl)

            new(internalNameOf<WorldEvent.Unload>())
            dup
            aload(0)
            getfield(
                internalNameOf<Minecraft>(),
                "theWorld",
                "L${internalNameOf<WorldClient>()};"
            )
            invokespecial(
                internalNameOf<WorldEvent.Unload>(),
                "<init>",
                "(L${internalNameOf<World>()};)V"
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
                "(L${internalNameOf<World>()};)V"
            )
            callEvent()

            +end
            f_same()
        })
    }
}
