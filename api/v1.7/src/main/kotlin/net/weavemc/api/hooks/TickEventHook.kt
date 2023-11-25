@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.getSingleton
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.TickEvent
import net.weavemc.api.getMappedClass
import net.weavemc.api.getMappedMethod
import net.weavemc.api.runtimeName
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * A [TickEvent] is posted every tick. A tick is a fixed interval of time defined in
 * [net.minecraft.util.Timer], every tick, various game mechanics are updated, such as
 * entity movement, block updates, and player movement,
 *
 * @see net.minecraft.util.Timer.ticksPerSecond
 */
class TickEventHook : Hook(getMappedClass("net/minecraft/client/Minecraft")) {

    /**
     * Inserts a call to the [net.minecraft.client.Minecraft.runTick] method to post
     * a 'tick'.
     *
     * @see net.minecraft.client.Minecraft.runTick
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "runTick",
            "()V"
        )

        val runTick = node.methods.search(mappedMethod.runtimeName, mappedMethod.desc)

        runTick.instructions.insert(asm {
            getSingleton<net.weavemc.api.event.TickEvent.Pre>()
            callEvent()
        })

        runTick.instructions.insertBefore(
            runTick.instructions.findLast { it.opcode == Opcodes.RETURN },
            asm {
                getSingleton<net.weavemc.api.event.TickEvent.Post>()
                callEvent()
            }
        )
    }
}
