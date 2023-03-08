package club.maxstats.weave.loader.api.event

import com.mojang.authlib.GameProfile
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S38PacketPlayerListItem.AddPlayerData
import net.minecraft.util.IChatComponent
import net.minecraft.world.World
import net.minecraft.world.WorldSettings.GameType

class TickEvent : Event()
class InputEvent(val keyCode: Int) : Event()
class ChatReceivedEvent(val message: IChatComponent) : CancellableEvent()
class GuiOpenEvent(val screen: GuiScreen?) : CancellableEvent()
class RenderGameOverlayEvent : Event()
sealed class EntityListEvent(val entity: Entity) : Event() {
    class Add(entity: Entity) : EntityListEvent(entity)
    class Remove(entity: Entity) : EntityListEvent(entity)
}
sealed class PlayerListEvent(val playerData: AddPlayerData) : Event() {
    class Add(playerData: AddPlayerData) : PlayerListEvent(playerData)
    class Remove(playerData: AddPlayerData) : PlayerListEvent(playerData)
}
sealed class RenderLivingEvent(val renderer: RendererLivingEntity<EntityLivingBase>, val entity: EntityLivingBase, val x: Double, val y: Double, val z: Double, val partialTicks: Float) : CancellableEvent() {
    class Pre(renderer: RendererLivingEntity<EntityLivingBase>, entity: EntityLivingBase, x: Double, y: Double, z: Double, partialTicks: Float) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)
    class Post(renderer: RendererLivingEntity<EntityLivingBase>, entity: EntityLivingBase, x: Double, y: Double, z: Double, partialTicks: Float) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)
}
class ShutdownEvent : Event()