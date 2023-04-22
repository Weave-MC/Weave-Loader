package net.weavemc.loader.api.event.client

import net.minecraft.client.gui.GuiScreen
import net.weavemc.loader.api.event.CancellableEvent

public class GuiOpenEvent(public val screen: GuiScreen?) : CancellableEvent()
