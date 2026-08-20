package net.weavemc.api.event

import org.lwjgl.input.Keyboard

/**
 * Keyboard Events are called when a key is pressed or released.
 */
public class KeyboardEvent : Event() {
    /**
     * The key code is the LWJGL2 key code for the key being pressed.
     *
     * @see org.lwjgl.input.Keyboard
     */
    public val keyCode: Int =
        if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256
        else Keyboard.getEventKey()

    /**
     * The key state indicates whether the key is being **pressed (`true`)** or **released (`false`)**.
     */
    public val keyState: Boolean = Keyboard.getEventKeyState()
}