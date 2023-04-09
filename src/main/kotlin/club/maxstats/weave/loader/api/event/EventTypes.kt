package club.maxstats.weave.loader.api.event

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S38PacketPlayerListItem.AddPlayerData
import net.minecraft.util.IChatComponent
import net.minecraft.world.World
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

public abstract class Event
public abstract class CancellableEvent : Event() {
    @get:JvmName("isCancelled")
    public var cancelled: Boolean = false

}

public object TickEvent : Event()
public class KeyboardEvent : Event() {

    public val keyCode: Int =
        if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256
        else Keyboard.getEventKey()

    public val keyState: Boolean = Keyboard.getEventKeyState()

}

public class MouseEvent : CancellableEvent() {

    public val x: Int  = Mouse.getEventX()
    public val y: Int  = Mouse.getEventY()

    @get:JvmName("getDX")
    public val dx: Int = Mouse.getEventDX()

    @get:JvmName("getDY")
    public val dy: Int = Mouse.getEventDY()

    @get:JvmName("getDWheel")
    public val dwheel: Int          = Mouse.getEventDWheel()
    public val button: Int          = Mouse.getEventButton()
    public val buttonState: Boolean = Mouse.getEventButtonState()
    public val nanoseconds: Long    = Mouse.getEventNanoseconds()

}

public class ChatReceivedEvent(public val message: IChatComponent) : CancellableEvent()
public class ChatSentEvent(public val message: String) : CancellableEvent()
public class GuiOpenEvent(public val screen: GuiScreen?) : CancellableEvent()

public sealed class RenderGameOverlayEvent(public val partialTicks: Float) : Event() {

    public class Pre(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)
    public class Post(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)

}

public sealed class EntityListEvent(public val entity: Entity) : Event() {

    public class Add(entity: Entity) : EntityListEvent(entity)
    public class Remove(entity: Entity) : EntityListEvent(entity)

}

public sealed class PlayerListEvent(public val playerData: AddPlayerData) : Event() {

    public class Add(playerData: AddPlayerData) : PlayerListEvent(playerData)
    public class Remove(playerData: AddPlayerData) : PlayerListEvent(playerData)

}

public sealed class RenderLivingEvent(
    public val renderer: RendererLivingEntity<EntityLivingBase>,
    public val entity: EntityLivingBase,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val partialTicks: Float
) : CancellableEvent() {

    public class Pre(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

    public class Post(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

}

public class RenderWorldEvent(public val partialTicks: Float) : Event()

public class RenderHandEvent(public val partialTicks: Float) : CancellableEvent()

public class ServerConnectEvent(
    public val ip: String,
    public val port: Int,
) : Event() {
    public val serverData: ServerData = Minecraft.getMinecraft().currentServerData
}

public sealed class StartGameEvent : Event() {

    public object Pre : StartGameEvent()
    public object Post : StartGameEvent()

}

public object ShutdownEvent : Event()

public sealed class WorldEvent(public val world: World) : Event() {
    public class Load(world: World) : WorldEvent(world)
    public class Unload(world: World) : WorldEvent(world)
}

public sealed class PacketEvent(public val packet: Packet<*>) : CancellableEvent() {
    public class Send(packet: Packet<*>) : PacketEvent(packet)
    public class Receive(packet: Packet<*>) : PacketEvent(packet)
}
