package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.TickEvent
import net.weavemc.api.bytecode.postEvent
import net.weavemc.internals.asm
import net.weavemc.internals.getSingleton
import net.weavemc.internals.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * A [TickEvent] is posted every tick. A tick is a fixed interval of time defined in
 * [net.minecraft.util.Timer], every tick, various game mechanics are updated, such as
 * entity movement, block updates, and player movement,
 *
 * @see net.minecraft.util.Timer.ticksPerSecond
 */
internal object TickEventHook : Hook("net/minecraft/client/Minecraft") {
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
            postEvent()
        })

        runTick.instructions.insertBefore(
            runTick.instructions.findLast { it.opcode == Opcodes.RETURN },
            asm {
                getSingleton<TickEvent.Post>()
                postEvent()
            }
        )
    }
}