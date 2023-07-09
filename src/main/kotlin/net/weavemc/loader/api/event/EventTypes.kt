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
 * receive a base Tick Event. These are each called every game tick, or every `50/timer` milliseconds (every 1/20th of a second at timer `1`).
 * The game's timer speed will never change (unless modified by cheats), so it is safe to assume that it is `1`.
 */
public sealed class TickEvent : Event() {

    /**
     * Pre Tick Events are called at the start of a tick.
     */
    public object Pre : TickEvent()

    /**
     * Post Tick Events are called at the end of a tick.
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
     *
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

/**
 * This cancellable event is called when a mouse action is performed.
 *
 * If cancelled, the event's actions will not affect the game.
 */
public class MouseEvent : CancellableEvent() {

    /**
     * The mouse's X position on the screen.
     */
    public val x: Int  = Mouse.getEventX()

    /**
     * The mouse's Y position on the screen.
     */
    public val y: Int  = Mouse.getEventY()

    /**
     * The X distance the mouse has travelled.
     */
    @get:JvmName("getDX")
    public val dx: Int = Mouse.getEventDX()

    /**
     * The Y distance the mouse has travelled.
     */
    @get:JvmName("getDY")
    public val dy: Int = Mouse.getEventDY()

    /**
     * The amount the mouse's scroll wheel has scrolled, negative if scrolling backwards.
     */
    @get:JvmName("getDWheel")
    public val dwheel: Int          = Mouse.getEventDWheel()

    /**
     * The mouse button the event is about.
     * 0. Left
     * 1. Right
     * 2. Middle
     */
    public val button: Int          = Mouse.getEventButton()

    /**
     * Whether the mouse button is being **pressed (`true`)** or **released (`false`)**.
     */
    public val buttonState: Boolean = Mouse.getEventButtonState()

    /**
     * The nanosecond that this mouse event was created. Obtained from the mouse event's
     * [Mouse.getEventNanoseconds()][Mouse.getEventNanoseconds], refer to the LWJGL2 Javadoc
     * for more information about it.
     */
    public val nanoseconds: Long    = Mouse.getEventNanoseconds()

}

/**
 * This cancellable event is called when your client receives a chat message from the server.
 *
 * @property message The message being received, in the form of a [Chat Component][IChatComponent].
 */
public class ChatReceivedEvent(public val message: IChatComponent) : CancellableEvent()

/**
 * This cancellable event is called when your client sends a chat message to the server.
 *
 * If cancelled, the message will not be sent, but still added to your chat history.
 * This can be useful for making your own command system.
 *
 * @property message The message that is going to be sent.
 */
public class ChatSentEvent(public val message: String) : CancellableEvent()

/**
 * This cancellable event is called when a [Gui Screen][GuiScreen] is opened.
 *
 * If cancelled, the screen will not be opened.
 *
 * @property screen The screen being opened.
 */
public class GuiOpenEvent(public val screen: GuiScreen?) : CancellableEvent()

/**
 * This event is called when the HUD (game overlay) is being rendered.
 *
 * It is split into [Pre] and [Post].
 */
public sealed class RenderGameOverlayEvent(public val partialTicks: Float) : Event() {

    /**
     * This is called **before** the game overlay renders, and should be used if you want to
     * draw to the screen without drawing over the HUD.
     */
    public class Pre(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)

    /**
     * This is called **after** the game overlay renders, and should be used to draw whatever
     * HUD components your mod needs.
     */
    public class Post(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)

}

/**
 * This event is called when an entity is added to or removed from the world.
 *
 * It is split into [Add] and [Remove].
 *
 * @property entity The entity being added/removed.
 */
public sealed class EntityListEvent(public val entity: Entity) : Event() {

    /**
     * This is called when an entity is added to the world.
     */
    public class Add(entity: Entity) : EntityListEvent(entity)

    /**
     * This is called when an entity is removed from the world.
     */
    public class Remove(entity: Entity) : EntityListEvent(entity)

}

/**
 * This event is called when a player is added or removed from the player list.
 *
 * This event is split into [Add] and [Remove].
 *
 * @property playerData The Player Data of the player being added/removed.
 */
public sealed class PlayerListEvent(public val playerData: AddPlayerData) : Event() {

    /**
     * This is called when a player is added to the player list.
     */
    public class Add(playerData: AddPlayerData) : PlayerListEvent(playerData)

    /**
     * This is called when a player is removed from the player list.
     */
    public class Remove(playerData: AddPlayerData) : PlayerListEvent(playerData)

}

/**
 * This event is called when an entity is rendered.
 *
 * It is split into [Pre] and [Post]. The [Pre] version of this event is cancellable.
 *
 * @property entity The entity being rendered.
 * @property x The `x` coordinate where the entity is being rendered this frame.
 * @property y The `y` coordinate where the entity is being rendered this frame.
 * @property z The `z` coordinate where the entity is being rendered this frame.
 */
public sealed class RenderLivingEvent(
    public val renderer: RendererLivingEntity<EntityLivingBase>,
    public val entity: EntityLivingBase,
    public val x: Double,
    public val y: Double,
    public val z: Double,
    public val partialTicks: Float
) : CancellableEvent() {

    /**
     * This is called before an entity is rendered.
     *
     * If cancelled, the entity is not rendered.
     */
    public class Pre(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

    /**
     * This is called after an entity is rendered.
     */
    public class Post(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

}

/**
 * Non-cancellable, called in [net.minecraft.client.renderer.EntityRenderer.renderWorldPass] after the world is rendered.
 * It is also called few lines before [RenderHandEvent].
 */
public class RenderWorldEvent(public val partialTicks: Float) : Event()

/**
 * Cancellable event, called in [net.minecraft.client.renderer.EntityRenderer.renderWorldPass] in the event
 * that your hand is rendered (crazy right).
 */
public class RenderHandEvent(public val partialTicks: Float) : CancellableEvent()

/**
 * Non-cancellable event, called at the head of [net.minecraft.client.multiplayer.GuiConnecting.connect], therefore, in the
 * event that the player connects to a server, this event is called along-side the player clicking the
 * connect to server button.
 *
 * @property ip   The IP address of the server.
 * @property port The port of the server.
 */
public class ServerConnectEvent(
    public val ip: String,
    public val port: Int,
) : Event() {

    /**
     * The [ServerData] object of the server being connected to.
     */
    public val serverData: ServerData = Minecraft.getMinecraft().currentServerData
}

/**
 * Non-cancellable event, split into [Pre] and [Post].
 */
public sealed class StartGameEvent : Event() {

    /**
     * Called in correspondence with [net.minecraft.client.Minecraft.startGame] at the head of the method.
     * Therefore, [Pre] is called early in the game startup process.
     */
    public object Pre : StartGameEvent()

    /**
     * Called in correspondence with [net.minecraft.client.Minecraft.startGame] at the tail of the method.
     * Therefore, [Post] is called late in the game startup process.
     */
    public object Post : StartGameEvent()

}

/**
 * Non-cancellable event,
 * Called in correspondence with [net.minecraft.client.Minecraft.shutdownMinecraftApplet].
 * Therefore, [ShutdownEvent] is called by the [EventBus] only in the event that
 * the client is being shut down.
 */
public object ShutdownEvent : Event()

/**
 * Non-cancellable event, split into [Load] and [Unload].
 *
 * Event call in the event of loading, or unloading a world.
 */
public sealed class WorldEvent(public val world: World) : Event() {

    /**
     * Called in correspondence with [net.minecraft.client.Minecraft.loadWorld]
     * if [net.minecraft.client.multiplayer.WorldClient] is not null.
     * Therefore, [Load] is called by the [EventBus] only in the event
     * that a client loads a new world. Whether that be a server connection,
     * or a singleplayer world.
     */
    public class Load(world: World) : WorldEvent(world)

    /**
     * Called in correspondence with [net.minecraft.client.Minecraft.loadWorld]
     * if [net.minecraft.client.Minecraft.theWorld] is not null.
     * Therefore, [Unload] is called by the [EventBus] only in the event
     * that a client unloads a world. Whether that be a server disconnection,
     * or a singleplayer world.
     */
    public class Unload(world: World) : WorldEvent(world)
}

/**
 * Cancellable event, split into [Send] and [Receive].
 *
 * Called in the event of a packet being sent or received to the client via [Minecraft.myNetworkManager]
 *
 * @property packet The packet being processed.
 */
public sealed class PacketEvent(public val packet: Packet<*>) : CancellableEvent() {

    /**
     * Called in correspondence with [net.minecraft.network.NetworkManager.sendPacket],
     * which is called in the event that the client sends a packet to the server.
     *
     * When cancelled, packets are not sent to the server.
     *
     * @param packet The packet being sent.
     */
    public class Send(packet: Packet<*>) : PacketEvent(packet)

    /**
     * Called in correspondence with [net.minecraft.network.NetworkManager.channelRead0],
     * which is called in the event that the client receives a packet from the server.
     *
     * When cancelled, packets are not processed by the client.
     *
     * @param packet The packet being received.
     */
    public class Receive(packet: Packet<*>) : PacketEvent(packet)
}
