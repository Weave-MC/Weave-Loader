package club.maxstats.weave.loader.api.event

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S38PacketPlayerListItem.AddPlayerData
import net.minecraft.util.IChatComponent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

abstract class Event
abstract class CancellableEvent : Event() {

    @get:JvmName("isCancelled")
    var cancelled = false
}

object TickEvent : Event()
class KeyboardEvent(
    val keyCode: Int,
    val keyState: Boolean
) : Event() {
    constructor() : this(
        if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256
        else Keyboard.getEventKey(),
        Keyboard.getEventKeyState()
    )
}

class MouseEvent(
    val x: Int,
    val y: Int,
    @get:JvmName("getDX") val dx: Int,
    @get:JvmName("getDY") val dy: Int,
    @get:JvmName("getDWheel") val dwheel: Int,
    val button: Int,
    val buttonState: Boolean,
    val nanoseconds: Long
) : CancellableEvent() {
    constructor() : this(
        Mouse.getEventX(),
        Mouse.getEventY(),
        Mouse.getEventDX(),
        Mouse.getEventDY(),
        Mouse.getEventDWheel(),
        Mouse.getEventButton(),
        Mouse.getEventButtonState(),
        Mouse.getEventNanoseconds()
    )
}

class ChatReceivedEvent(val message: IChatComponent) : CancellableEvent()
class ChatSentEvent(val message: String) : CancellableEvent()
class GuiOpenEvent(val screen: GuiScreen?) : CancellableEvent()

sealed class RenderGameOverlayEvent(val partialTicks: Float) : Event() {
    class Pre(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)
    class Post(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)
}

sealed class EntityListEvent(val entity: Entity) : Event() {
    class Add(entity: Entity) : EntityListEvent(entity)
    class Remove(entity: Entity) : EntityListEvent(entity)
}

sealed class PlayerListEvent(val playerData: AddPlayerData) : Event() {
    class Add(playerData: AddPlayerData) : PlayerListEvent(playerData)
    class Remove(playerData: AddPlayerData) : PlayerListEvent(playerData)
}

sealed class RenderLivingEvent(
    val renderer: RendererLivingEntity<EntityLivingBase>,
    val entity: EntityLivingBase,
    val x: Double,
    val y: Double,
    val z: Double,
    val partialTicks: Float
) : CancellableEvent() {
    class Pre(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

    class Post(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)
}

class RenderWorldEvent(val partialTicks: Float) : Event()

class RenderHandEvent(val partialTicks: Float) : CancellableEvent()

sealed class StartGameEvent : Event() {
    object Pre : StartGameEvent()
    object Post : StartGameEvent()
}

object ShutdownEvent : Event()
