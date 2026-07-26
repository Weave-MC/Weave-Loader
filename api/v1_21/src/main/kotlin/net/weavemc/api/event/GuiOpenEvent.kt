package net.weavemc.api.event

import net.minecraft.client.gui.screen.Screen

/**
 * This cancellable event is called when a [Gui Screen][Screen] is opened.
 *
 * If cancelled, the screen will not be opened.
 *
 * @property screen The screen being opened.
 */
public class GuiOpenEvent(public val screen: Screen?) : CancellableEvent()