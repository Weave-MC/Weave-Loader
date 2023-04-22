package net.weavemc.loader.hooks.client

import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.event.client.TickEvent
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.callEvent
import net.weavemc.loader.util.getSingleton
import net.weavemc.loader.util.named
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
