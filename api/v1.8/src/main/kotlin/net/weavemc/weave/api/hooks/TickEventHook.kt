@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.weave.api.hooks

import net.weavemc.weave.api.Hook
import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.callEvent
import net.weavemc.weave.api.bytecode.getSingleton
import net.weavemc.weave.api.bytecode.named
import net.weavemc.weave.api.event.TickEvent
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * A [TickEvent] is posted every tick. A tick is a fixed interval of time defined in
 * [net.minecraft.util.Timer], every tick, various game mechanics are updated, such as
 * entity movement, block updates, and player movement,
 *
 * @see net.minecraft.util.Timer.ticksPerSecond
 */
internal class TickEventHook : Hook("net/minecraft/client/Minecraft") {

    /**
     * Inserts a call to the [net.minecraft.client.Minecraft.runTick] method to post
     * a 'tick'.
     *
     * @see net.minecraft.client.Minecraft.runTick
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val runTick = node.methods.named("runTick")
        runTick.instructions.insert(asm {
            getSingleton<TickEvent.Pre>()
            callEvent()
        })

        runTick.instructions.insertBefore(
            runTick.instructions.findLast { it.opcode == Opcodes.RETURN },
            asm {
                getSingleton<TickEvent.Post>()
                callEvent()
            }
        )
    }
}
