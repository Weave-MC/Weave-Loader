package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.WeaveLoader
import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.StartGameEvent
import club.maxstats.weave.loader.api.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

internal class ModInitializerHook : Hook("net/minecraft/client/Minecraft") {

    /**
     * Inserts a call to [net.minecraft.client.Minecraft.startGame] to initialize
     * Weave mods before the return statement in the startGame method.
     *
     * @see net.minecraft.client.Minecraft.startGame
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mn = node.methods.named("startGame")
        mn.instructions.insertBefore(mn.instructions.find { it.opcode == Opcodes.RETURN }, asm {
            invokestatic(
                internalNameOf<WeaveLoader>(),
                "initMods",
                "()V"
            )
        })
    }
}
