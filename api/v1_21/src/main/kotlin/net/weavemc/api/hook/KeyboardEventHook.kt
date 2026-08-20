package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.event.KeyboardEvent
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

internal class KeyboardEventHook : Hook("net/minecraft/client/Keyboard") {
    /**
     * @see net.minecraft.client.Keyboard.onKey
     */
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        node.methods.named("onKey").instructions.insert(asm {
            new(internalNameOf<KeyboardEvent>())
            dup
            aload(4)
            invokevirtual("net/minecraft/client/input/KeyInput", "comp_4795", "()I") // key
            aload(4)
            invokevirtual("net/minecraft/client/input/KeyInput", "comp_4796", "()I") // scancode
            iload(3)
            aload(4)
            invokevirtual("net/minecraft/client/input/KeyInput", "comp_4797", "()I") // modifiers
            invokespecial(
                internalNameOf<KeyboardEvent>(),
                "<init>",
                "(IIII)V"
            )
            postEvent()
        })
    }
}