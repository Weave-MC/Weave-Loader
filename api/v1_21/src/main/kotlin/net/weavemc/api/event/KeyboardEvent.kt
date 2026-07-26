package net.weavemc.api.event

import org.lwjgl.glfw.GLFW

/**
 * Keyboard Events are called when a key is pressed or released.
 *
 * @property key The GLFW key code (e.g., GLFW.GLFW_KEY_W).
 * @property scancode The platform-specific scancode.
 * @property action The GLFW action: [GLFW.GLFW_PRESS] (1), [GLFW.GLFW_RELEASE] (0), or [GLFW.GLFW_REPEAT] (2).
 * @property modifiers Bitfield describing which modifier keys (Shift, Ctrl, Alt) were held down.
 */
public class KeyboardEvent(
    public val key: Int,
    public val scancode: Int,
    public val action: Int,
    public val modifiers: Int
) : Event() {
    /**
     * Indicates whether the key is currently being **pressed (`true`)** or **released (`false`)**.
     * Note: GLFW also has repeat actions (`action == GLFW.GLFW_REPEAT`).
     */
    public val keyState: Boolean get() = action != GLFW.GLFW_RELEASE
}