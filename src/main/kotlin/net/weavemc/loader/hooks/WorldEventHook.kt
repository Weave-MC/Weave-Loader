package net.weavemc.loader.hooks

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.client.WorldEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.internalNameOf
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode

public class WorldEventHook: Hook("net/minecraft/client/Minecraft") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.find {
            it.name == "loadWorld" && it.desc == "(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V"
        }!!.instructions.insert(asm {
            val lbl = LabelNode()

            aload(0)
            getfield("net/minecraft/client/Minecraft", "theWorld", "Lnet/minecraft/client/multiplayer/WorldClient;")
            ifnull(lbl)

            new(internalNameOf<WorldEvent.Unload>())
            dup
            aload(0)
            getfield("net/minecraft/client/Minecraft", "theWorld", "Lnet/minecraft/client/multiplayer/WorldClient;")
            invokespecial(internalNameOf<WorldEvent.Unload>(), "<init>", "(Lnet/minecraft/world/World;)V")
            callEvent()

            +lbl
            f_same()

            val end = LabelNode()
            aload(1)
            ifnull(end)

            new(internalNameOf<WorldEvent.Load>())
            dup
            aload(1)
            invokespecial(internalNameOf<WorldEvent.Load>(), "<init>", "(Lnet/minecraft/world/World;)V")
            callEvent()

            +end
            f_same()
        })
    }
}
