package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.TickEvent
import club.maxstats.weave.loader.api.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.named
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
     * Inserts a call to the [net.minecraft.client.runTick] method to post
     * a 'tick'.
     *
     * @see net.minecraft.client.Minecraft.runTick
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("runTick").instructions.insert(asm {
            getSingleton<TickEvent>()
            callEvent()
        })
    }
}
