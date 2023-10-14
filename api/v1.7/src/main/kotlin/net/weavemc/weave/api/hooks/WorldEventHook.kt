@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.*
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.search
import net.weavemc.weave.api.event.WorldEvent
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

/**
 * Corresponds to [WorldEvent.Load] and [WorldEvent.Unload].
 */
class WorldEventHook: Hook(getMappedClass("net/minecraft/client/Minecraft")) {

    /**
     * Inserts a call in [net.minecraft.client.Minecraft.loadWorld] to [WorldEvent.Load] and later [WorldEvent.Unload].
     *
     * @see net.minecraft.client.Minecraft.loadWorld
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val minecraftName = getMappedClass("net/minecraft/client/Minecraft")

        val mappedMethod = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "loadWorld",
            "(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V"
        )

        val theWorld = getMappedField(
            "net/minecraft/client/Minecraft",
            "theWorld"
        )

        node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)
            .instructions.insert(asm {
                val lbl = LabelNode()

                aload(0)
                getfield(
                    minecraftName,
                    theWorld.runtimeName,
                    "L${getMappedClass("net/minecraft/client/multiplayer/WorldClient")};"
                )
                ifnull(lbl)

                new(internalNameOf<WorldEvent.Unload>())
                dup
                aload(0)
                getfield(
                    minecraftName,
                    theWorld.runtimeName,
                    "L${getMappedClass("net/minecraft/client/multiplayer/WorldClient")};"
                )
                invokespecial(
                    internalNameOf<WorldEvent.Unload>(),
                    "<init>",
                    "(L${getMappedClass("net/minecraft/world/World")};)V"
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
                    "(L${getMappedClass("net/minecraft/world/World")};)V"
                )
                callEvent()

                +end
                f_same()
            })
    }
}
