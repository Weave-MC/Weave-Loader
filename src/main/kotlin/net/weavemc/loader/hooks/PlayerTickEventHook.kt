package net.weavemc.loader.hooks

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.getSingleton
import net.weavemc.loader.util.insertBeforeReturn
import net.weavemc.loader.util.named
import org.objectweb.asm.tree.ClassNode

internal class PlayerTickEventHook : Hook("net/minecraft/entity/player/EntityPlayer") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val onUpdate = node.methods.named("onUpdate")
        onUpdate.instructions.insert(asm {
            getSingleton<net.weavemc.loader.api.event.PlayerTickEvent.Pre>()
            callEvent()
        })

        onUpdate.instructions.insertBeforeReturn(asm {
            getSingleton<net.weavemc.loader.api.event.PlayerTickEvent.Post>()
            callEvent()
        })
    }
}
