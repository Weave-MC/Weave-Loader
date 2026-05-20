package net.weavemc.api.event

import net.minecraft.client.gui.GuiScreen

/**
 * This cancellable event is called when a [Gui Screen][net.minecraft.client.gui.GuiScreen] is opened.
 *
 * If cancelled, the screen will not be opened.
 *
 * @property screen The screen being opened.
 */
class GuiOpenEvent(val screen: GuiScreen?) : CancellableEvent()