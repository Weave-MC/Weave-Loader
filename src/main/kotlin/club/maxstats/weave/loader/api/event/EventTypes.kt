package club.maxstats.weave.loader.api.event

import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.IChatComponent

class TickEvent : Event()
class InputEvent(val keyCode: Int) : Event()
class ChatReceivedEvent(val message: IChatComponent) : Event()
class GuiOpenEvent(val screen: GuiScreen) : Event()
class ShutdownEvent : Event()