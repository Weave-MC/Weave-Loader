package net.weavemc.loader.api.event.client

import net.weavemc.loader.api.event.CancellableEvent
import net.weavemc.loader.api.event.Event
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

public class KeyboardEvent : Event() {
    public val keyCode: Int =
        if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256
        else Keyboard.getEventKey()

    public val keyState: Boolean = Keyboard.getEventKeyState()
}

public class MouseEvent : CancellableEvent() {
    public val x: Int  = Mouse.getEventX()
    public val y: Int  = Mouse.getEventY()

    @get:JvmName("getDX")
    public val dx: Int = Mouse.getEventDX()

    @get:JvmName("getDY")
    public val dy: Int = Mouse.getEventDY()

    @get:JvmName("getDWheel")
    public val dwheel: Int          = Mouse.getEventDWheel()
    public val button: Int          = Mouse.getEventButton()
    public val buttonState: Boolean = Mouse.getEventButtonState()
    public val nanoseconds: Long    = Mouse.getEventNanoseconds()
}
