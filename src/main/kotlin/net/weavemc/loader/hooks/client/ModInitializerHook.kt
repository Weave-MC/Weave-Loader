package net.weavemc.loader.hooks.client

import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.api.Hook
import net.weavemc.loader.api.util.asm
import net.weavemc.loader.util.internalNameOf
import net.weavemc.loader.util.named
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
