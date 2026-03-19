package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.StartGameEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.internals.asm
import net.weavemc.internals.getSingleton
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [StartGameEvent.Pre] and [StartGameEvent.Post].
 */
internal class StartGameEventHook : Hook("net/minecraft/client/Minecraft") {
    /**
     * Inserts a call to [net.minecraft.client.Minecraft.startGame] using the Event Bus.
     *
     * [StartGameEvent.Pre] is called at the head of [net.minecraft.client.Minecraft.startGame]. Whereas
     * [StartGameEvent.Post] is called at the tail of [net.minecraft.client.Minecraft.startGame].
     *
     * @see net.minecraft.client.Minecraft.startGame
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("startGame")

        mn.instructions.insert(asm {
            getSingleton<StartGameEvent.Pre>()
            postEvent()
        })

        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == RETURN }, asm {
            getSingleton<StartGameEvent.Post>()
            postEvent()
        })
    }
}
