package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.TickEvent
import net.weavemc.internals.asm
import net.weavemc.internals.getSingleton
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * A [TickEvent] is posted every tick. Ticks occur at a fixed target rate (typically 20 ticks per second)
 * managed by [net.minecraft.client.render.RenderTickCounter]. During each tick, various client-side game mechanics
 * are updated, such as entity movement, block updates, and world rendering states.
 *
 * @see net.minecraft.client.render.RenderTickCounter
 */
internal class TickEventHook : Hook("net/minecraft/client/MinecraftClient") {
    /**
     * Inserts calls to post [TickEvent.Pre] and [TickEvent.Post] at the head and return points
     * of [net.minecraft.client.MinecraftClient.tick].
     *
     * @see net.minecraft.client.MinecraftClient.tick
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("tick")

        mn.instructions.insert(asm {
            getSingleton<TickEvent.Pre>()
            postEvent()
        })

        mn.instructions.insertBefore(mn.instructions.findLast { it.opcode == Opcodes.RETURN }, asm {
            getSingleton<TickEvent.Post>()
            postEvent()
        })
    }
}