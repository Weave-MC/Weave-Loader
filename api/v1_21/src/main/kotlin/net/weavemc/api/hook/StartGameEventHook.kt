package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.StartGameEvent
import net.weavemc.internals.asm
import net.weavemc.internals.getSingleton
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [StartGameEvent.Pre] and [StartGameEvent.Post].
 */
internal class StartGameEventHook : Hook("net/minecraft/client/MinecraftClient") {
    /**
     * Inserts calls to post [StartGameEvent.Pre] and [StartGameEvent.Post]
     * at the head and return points of the [net.minecraft.client.MinecraftClient] constructor.
     *
     * @see net.minecraft.client.MinecraftClient
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("<init>")

        mn.instructions.insert(asm {
            getSingleton<StartGameEvent.Pre>()
            postEvent()
        })

        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == Opcodes.RETURN }, asm {
            getSingleton<StartGameEvent.Post>()
            postEvent()
        })
    }
}
