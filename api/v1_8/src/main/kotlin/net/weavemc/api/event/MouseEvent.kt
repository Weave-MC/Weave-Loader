package net.weavemc.api.event

import org.lwjgl.input.Mouse

/**
 * This cancellable event is called when a mouse action is performed.
 *
 * If cancelled, the event's actions will not affect the game.
 */
class MouseEvent : CancellableEvent() {
    /**
     * The mouse's X position on the screen.
     */
    val x: Int = Mouse.getEventX()

    /**
     * The mouse's Y position on the screen.
     */
    val y: Int = Mouse.getEventY()

    /**
     * The X distance the mouse has travelled.
     */
    @get:JvmName("getDX")
    val dx: Int = Mouse.getEventDX()

    /**
     * The Y distance the mouse has travelled.
     */
    @get:JvmName("getDY")
    val dy: Int = Mouse.getEventDY()

    /**
     * The amount the mouse's scroll wheel has scrolled, negative if scrolling backwards.
     */
    @get:JvmName("getDWheel")
    val dwheel: Int = Mouse.getEventDWheel()

    /**
     * The mouse button the event is about.
     * 0. Left
     * 1. Right
     * 2. Middle
     */
    val button: Int = Mouse.getEventButton()

    /**
     * Whether the mouse button is being **pressed (`true`)** or **released (`false`)**.
     */
    val buttonState: Boolean = Mouse.getEventButtonState()

    /**
     * The nanosecond that this mouse event was created. Obtained from the mouse event's
     * [Mouse.getEventNanoseconds()][Mouse.getEventNanoseconds], refer to the LWJGL2 Javadoc
     * for more information about it.
     */
    val nanoseconds: Long = Mouse.getEventNanoseconds()
}