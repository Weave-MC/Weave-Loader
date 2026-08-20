package net.weavemc.api.event

import org.lwjgl.glfw.GLFW

/**
 * Represents any mouse action performed inside the window.
 *
 * @property x The mouse's current X position on the window (in double precision).
 * @property y The mouse's current Y position on the window (in double precision).
 */
public sealed class MouseEvent(
    public val x: Double,
    public val y: Double
) : CancellableEvent() {
    /**
     * Called when a mouse button is pressed or released.
     *
     * @property button The mouse button (0: Left, 1: Right, 2: Middle, etc.).
     * @property action The GLFW action: [GLFW.GLFW_PRESS] (1) or [GLFW.GLFW_RELEASE] (0).
     * @property modifiers Bitfield describing which modifier keys (Shift, Ctrl, Alt) were held down.
     */
    public class Click(
        x: Double,
        y: Double,
        public val button: Int,
        public val action: Int,
        public val modifiers: Int
    ) : MouseEvent(x, y) {
        /**
         * `true` if the button was pressed, `false` if released.
         */
        public val buttonState: Boolean get() = action != GLFW.GLFW_RELEASE
    }

    /**
     * Called when the mouse cursor moves across the window.
     *
     * @property oldX The horizontal position before this move event.
     * @property oldY The vertical position before this move event.
     * @property x The new horizontal position after this move event.
     * @property y The new vertical position after this move event.
     */
    public class Move(
        public val oldX: Double,
        public val oldY: Double,
        x: Double,
        y: Double
    ) : MouseEvent(x, y) {
        /**
         * The horizontal delta movement.
         */
        public val dx: Double get() = x - oldX

        /**
         * The vertical delta movement.
         */
        public val dy: Double get() = y - oldY
    }

    /**
     * Called when the mouse scroll wheel is used.
     *
     * @property offsetX The horizontal scroll offset delta (xoffset).
     * @property offsetY The vertical scroll offset delta (yoffset) (positive = up, negative = down).
     */
    public class Scroll(
        x: Double,
        y: Double,
        public val offsetX: Double,
        public val offsetY: Double
    ) : MouseEvent(x, y)
}