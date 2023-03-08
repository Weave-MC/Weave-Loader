package club.maxstats.weave.loader.api.event

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.IChatComponent

class TickEvent : Event()
class InputEvent(val keyCode: Int) : Event()
class ChatReceivedEvent(val message: IChatComponent) : CancellableEvent()
class GuiOpenEvent(val screen: GuiScreen?) : CancellableEvent()
abstract class RenderLivingEvent(val renderer: RendererLivingEntity<EntityLivingBase>, val entity: EntityLivingBase, val x: Double, val y: Double, val z: Double, val partialTicks: Float) : CancellableEvent() {
    class Pre(renderer: RendererLivingEntity<EntityLivingBase>, entity: EntityLivingBase, x: Double, y: Double, z: Double, partialTicks: Float) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)
    class Post(renderer: RendererLivingEntity<EntityLivingBase>, entity: EntityLivingBase, x: Double, y: Double, z: Double, partialTicks: Float) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)
}
class ShutdownEvent : Event()