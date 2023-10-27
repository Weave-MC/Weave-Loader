@file:Suppress("invisible_reference", "invisible_member")

package net.weavemc.api.hooks

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.callEvent
import net.weavemc.api.bytecode.getSingleton
import net.weavemc.api.bytecode.search
import net.weavemc.api.event.ShutdownEvent
import net.weavemc.weave.api.getMappedClass
import net.weavemc.weave.api.getMappedMethod
import net.weavemc.weave.api.runtimeName
import org.objectweb.asm.tree.ClassNode

/**
 * Corresponds to [ShutdownEvent].
 */
class ShutdownEventHook : Hook(getMappedClass("net/minecraft/client/Minecraft")) {

    /**
     * Inserts a singleton shutdown call at the head of
     * [net.minecraft.client.Minecraft.shutdownMinecraftApplet].
     *
     * @see net.minecraft.client.Minecraft.shutdownMinecraftApplet
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val mappedMethod = getMappedMethod(
            "net/minecraft/client/Minecraft",
            "shutdownMinecraftApplet",
            "()V"
        )

        node.methods.search(mappedMethod.runtimeName, mappedMethod.desc).instructions.insert(asm {
            getSingleton<net.weavemc.api.event.ShutdownEvent>()
            callEvent()
        })
    }
}
