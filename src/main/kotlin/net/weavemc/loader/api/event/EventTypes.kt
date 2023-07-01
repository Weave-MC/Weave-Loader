package net.weavemc.loader.api.event

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

/**
 * This is the base class for all events provided by the Weave Loader.
 */
public abstract class Event

/**
 * This is the base class for all *cancellable* events provided by the
 * Weave Loader, extending [Event].
 */
public abstract class CancellableEvent : Event() {

    /**
     * This field defines whether the event is cancelled or not. Any mod can cancel and
     * un-cancel an event. What an event does when cancelled is event-specific, and noted in
     * that event's documentation.
     */
    @get:JvmName("isCancelled")
    public var cancelled: Boolean = false

}

/**
 * This is the base class for a Tick Event. Tick Events can be Pre and Post, but you will never
 * receive a base Tick Event. These are each called every game tick, or roughly every `50/timer` milliseconds.
 * The game's timer speed will never change (unless modified by cheats), so it is safe to assume that it is `1`.
 */
public sealed class TickEvent : Event() {

    /**
     * Pre Tick Events are called right before the game sends that tick's **position, rotation,
     * and onGround** updates to the server, and as such, they can be modified here. **If you
     * modify any of them here, it might be a good idea to return them to their original value on
     * the following [Post Tick Event][Post]**.
     */
    public object Pre : TickEvent()

    /**
     * Post Tick Events are called right after the game sends that tick's **position, rotation,
     * and onGround** updates to the server. Most player actions are done on [Pre], so
     * performing actions with the player here might cause flags with anti-cheats.
     */
    public object Post: TickEvent()
}

/**
 * Keyboard Events are called when a key is pressed or released while [currentScreen][Minecraft.currentScreen]
 * is `null`.
 */
public class KeyboardEvent : Event() {

    /**
     * The key code is the LWJGL2 key code for the key being pressed.
     * @see Keyboard
     */
    public val keyCode: Int =
        if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256
        else Keyboard.getEventKey()

    /**
     * The key state indicates whether the key is being **pressed (`true`)** or **released (`false`)**.
     */
    public val keyState: Boolean = Keyboard.getEventKeyState()

}

// todo: document this, im skipping it because i have no clue what half of these fields are
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

/**
 * This cancellable event is called when your client receives a chat message from the server.
 *
 * If cancelled, the message will not be displayed in chat.
 * @param message The message being received, in the form of a [Chat Component][IChatComponent].
 */
public class ChatReceivedEvent(public val message: IChatComponent) : CancellableEvent()

/**
 * This cancellable event is called when your client sends a chat message to the server.
 *
 * If cancelled, the message will not be sent, but still added to your chat history.
 * This can be useful for making your own command system.
 *
 * @param message The message that is going to be sent.
 */
public class ChatSentEvent(public val message: String) : CancellableEvent()

/**
 * This cancellable event is called when a [Gui Screen][GuiScreen] is opened.
 *
 * If cancelled, the screen will not be opened.
 *
 * @param screen The screen being opened.
 */
public class GuiOpenEvent(public val screen: GuiScreen?) : CancellableEvent()

// todo i dont understand half these events
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

/**
 * This event is called when you connect to a server.
 *
 * @param ip The IP address of the server.
 * @param port The port of the server.
 */
public class ServerConnectEvent(
    public val ip: String,
    public val port: Int,
) : Event() {

    /**
     * The [Server Data][ServerData] object of the server being connected to.
     */
    public val serverData: ServerData = Minecraft.getMinecraft().currentServerData
}

/**
 * This event is called when the game is starting. Like [Tick Event][TickEvent], it
 * is split into [Pre] and [Post].
 */
public sealed class StartGameEvent : Event() {

    /**
     * This is called before the game starts, at the beginning of [startGame][Minecraft.startGame].
     */
    public object Pre : StartGameEvent()

    /**
     * This is called after the game starts, at the end of [startGame][Minecraft.startGame].
     */
    public object Post : StartGameEvent()

}

/**
 * This event is called when the game shuts down. You might want to avoid saving
 * settings/data **only** on this event, as it might lead to possible data loss.
 */
public object ShutdownEvent : Event()

// todo write something for this
public sealed class WorldEvent(public val world: World) : Event() {
    public class Load(world: World) : WorldEvent(world)
    public class Unload(world: World) : WorldEvent(world)
}

/**
 * This cancellable event is called when packets are being handled by
 * the [Network Manager][Minecraft.myNetworkManager].
 *
 * This event is split into [Send] and [Receive].
 *
 * @param packet The packet being processed.
 */
public sealed class PacketEvent(public val packet: Packet<*>) : CancellableEvent() {

    /**
     * This is called when a packet is being sent by the client, to the server.
     *
     * If cancelled, the packet is not sent.
     */
    public class Send(packet: Packet<*>) : PacketEvent(packet)

    /**
     * This is called when a packet is being received by the client, from the server.
     *
     * If cancelled, the packet will not be processed, but instead ignored.
     */
    public class Receive(packet: Packet<*>) : PacketEvent(packet)
}
