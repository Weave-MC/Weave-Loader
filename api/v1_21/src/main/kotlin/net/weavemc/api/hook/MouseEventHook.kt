package net.weavemc.api.hook

import net.weavemc.api.Hook
import net.weavemc.api.bytecode.postEvent
import net.weavemc.api.bytecode.returnIfEventCancelled
import net.weavemc.api.event.MouseEvent
import net.weavemc.internals.InsnBuilder
import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.named
import org.objectweb.asm.tree.ClassNode

internal class MouseEventHook : Hook("net/minecraft/client/Mouse") {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        transformOnMouseButton(node)
        transformOnCursorPos(node)
        transformOnMouseScroll(node)
    }

    /**
     * @see net.minecraft.client.Mouse.onMouseButton
     */
    private fun transformOnMouseButton(node: ClassNode) {
        node.methods.named("onMouseButton").instructions.insert(asm {
            new(internalNameOf<MouseEvent.Click>())
            dup
            dup
            loadPosXY()
            aload(3)
            invokevirtual("net/minecraft/client/input/MouseInput", "button", "()I")
            iload(4)
            aload(3)
            invokevirtual("net/minecraft/client/input/MouseInput", "comp_4797", "()I") // modifiers
            invokespecial(
                internalNameOf<MouseEvent.Click>(),
                "<init>",
                "(DDIII)V"
            )
            postEvent()

            returnIfEventCancelled()
        })
    }

    /**
     * @see net.minecraft.client.Mouse.onCursorPos
     */
    private fun transformOnCursorPos(node: ClassNode) {
        node.methods.named("onCursorPos").instructions.insert(asm {
            new(internalNameOf<MouseEvent.Move>())
            dup
            loadPosXY()
            dload(3)
            dload(5)
            invokespecial(
                internalNameOf<MouseEvent.Move>(),
                "<init>",
                "(DDDD)V"
            )
            dup
            postEvent()

            returnIfEventCancelled()
        })
    }

    /**
     * @see net.minecraft.client.Mouse.onMouseScroll
     */
    private fun transformOnMouseScroll(node: ClassNode) {
        node.methods.named("onMouseScroll").instructions.insert(asm {
            new(internalNameOf<MouseEvent.Scroll>())
            dup
            dup
            loadPosXY()
            dload(3)
            dload(5)
            invokespecial(
                internalNameOf<MouseEvent.Scroll>(),
                "<init>",
                "(DDDD)V"
            )
            postEvent()

            returnIfEventCancelled()
        })
    }

    private fun InsnBuilder.loadPosXY() {
        aload(0)
        getfield("net/minecraft/client/Mouse", "x", "D")
        aload(0)
        getfield("net/minecraft/client/Mouse", "y", "D")
    }
}