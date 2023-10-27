package net.weavemc.api.event

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
 * This is the base class for a Tick Event. Tick Events can be Pre and Post, but you will never
 * receive a base Tick Event. These are each called every game tick, or every `50/timer` milliseconds (every 1/20th of a second at timer `1`).
 * The game's timer speed will never change (unless modified by cheats), so it is safe to assume that it is `1`.
 */
sealed class TickEvent : Event() {

    /**
     * Pre Tick Events are called at the start of a tick.
     */
    data object Pre : TickEvent()

    /**
     * Post Tick Events are called at the end of a tick.
     */
    data object Post: TickEvent()
}

/**
 * Keyboard Events are called when a key is pressed or released while [currentScreen][Minecraft.currentScreen]
 * is `null`.
 */
class KeyboardEvent : Event() {

    /**
     * The key code is the LWJGL2 key code for the key being pressed.
     *
     * @see Keyboard
     */
    val keyCode: Int =
        if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256
        else Keyboard.getEventKey()

    /**
     * The key state indicates whether the key is being **pressed (`true`)** or **released (`false`)**.
     */
    val keyState: Boolean = Keyboard.getEventKeyState()

}

/**
 * This cancellable event is called when a mouse action is performed.
 *
 * If cancelled, the event's actions will not affect the game.
 */
class MouseEvent : CancellableEvent() {
    /**
     * The mouse's X position on the screen.
     */
    val x: Int  = Mouse.getEventX()

    /**
     * The mouse's Y position on the screen.
     */
    val y: Int  = Mouse.getEventY()

    /**
     * The amount the mouse's scroll wheel has scrolled, negative if scrolling backwards.
     */
    @get:JvmName("getDWheel")
    val dwheel: Int          = Mouse.getEventDWheel()

    /**
     * The mouse button the event is about.
     * 0. Left
     * 1. Right
     * 2. Middle
     */
    val button: Int          = Mouse.getEventButton()

    /**
     * Whether the mouse button is being **pressed (`true`)** or **released (`false`)**.
     */
    val buttonState: Boolean = Mouse.getEventButtonState()

    /**
     * The nanosecond that this mouse event was created. Obtained from the mouse event's
     * [Mouse.getEventNanoseconds()][Mouse.getEventNanoseconds], refer to the LWJGL2 Javadoc
     * for more information about it.
     */
    val nanoseconds: Long    = Mouse.getEventNanoseconds()

}

/**
 * This cancellable event is called when your client receives a chat message from the server.
 *
 * @property message The message being received, in the form of a [Chat Component][IChatComponent].
 */
class ChatReceivedEvent(val message: IChatComponent) : CancellableEvent()

/**
 * This cancellable event is called when your client sends a chat message to the server.
 *
 * If cancelled, the message will not be sent, but still added to your chat history.
 * This can be useful for making your own command system.
 *
 * @property message The message that is going to be sent.
 */
class ChatSentEvent(val message: String) : CancellableEvent()

/**
 * This cancellable event is called when a [Gui Screen][GuiScreen] is opened.
 *
 * If cancelled, the screen will not be opened.
 *
 * @property screen The screen being opened.
 */
class GuiOpenEvent(val screen: GuiScreen?) : CancellableEvent()

/**
 * This event is called when the HUD (game overlay) is being rendered.
 *
 * It is split into [Pre] and [Post].
 */
sealed class RenderGameOverlayEvent(val partialTicks: Float) : Event() {

    /**
     * This is called **before** the game overlay renders, and should be used if you want to
     * draw to the screen without drawing over the HUD.
     */
    class Pre(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)

    /**
     * This is called **after** the game overlay renders, and should be used to draw whatever
     * HUD components your mod needs.
     */
    class Post(partialTicks: Float) : RenderGameOverlayEvent(partialTicks)

}

/**
 * This event is called when an entity is added to or removed from the world.
 *
 * It is split into [Add] and [Remove].
 *
 * @property entity The entity being added/removed.
 */
sealed class EntityListEvent(val entity: Entity) : Event() {

    /**
     * This is called when an entity is added to the world.
     */
    class Add(entity: Entity) : EntityListEvent(entity)

    /**
     * This is called when an entity is removed from the world.
     */
    class Remove(entity: Entity) : EntityListEvent(entity)

}

/**
 * This event is called when a player is added or removed from the player list.
 *
 * This event is split into [Add] and [Remove].
 *
 * @property playerData The Player Data of the player being added/removed.
 */
sealed class PlayerListEvent(val playerData: AddPlayerData) : Event() {

    /**
     * This is called when a player is added to the player list.
     */
    class Add(playerData: AddPlayerData) : PlayerListEvent(playerData)

    /**
     * This is called when a player is removed from the player list.
     */
    class Remove(playerData: AddPlayerData) : PlayerListEvent(playerData)

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
sealed class RenderLivingEvent(
    val renderer: RendererLivingEntity<EntityLivingBase>,
    val entity: EntityLivingBase,
    val x: Double,
    val y: Double,
    val z: Double,
    val partialTicks: Float
) : CancellableEvent() {

    /**
     * This is called before an entity is rendered.
     *
     * If cancelled, the entity is not rendered.
     */
    class Pre(
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
    class Post(
        renderer: RendererLivingEntity<EntityLivingBase>,
        entity: EntityLivingBase,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float
    ) : RenderLivingEvent(renderer, entity, x, y, z, partialTicks)

}

/**
 * This event is called when the world is being rendered.
 */
class RenderWorldEvent(val partialTicks: Float) : Event()

/**
 * This cancellable event is called when your player's hand is being rendered in 1st person.
 *
 * If cancelled, the hand will not be rendered.
 */
class RenderHandEvent(val partialTicks: Float) : CancellableEvent()

/**
 * This event is called when you connect to a server.
 *
 * @property ip The IP address of the server.
 * @property port The port of the server.
 */
class ServerConnectEvent(
    val ip: String,
    val port: Int,
) : Event() {

    /**
     * The [Server Data][ServerData] object of the server being connected to.
     */
    val serverData: ServerData = Minecraft.getMinecraft().currentServerData
}

/**
 * This event is called when the game is starting.
 *
 * This event is split into [Pre] and [Post].
 */
sealed class StartGameEvent : Event() {

    /**
     * This is called before the game starts, at the beginning of [startGame][Minecraft.startGame].
     */
    data object Pre : StartGameEvent()

    /**
     * This is called after the game starts, at the end of [startGame][Minecraft.startGame].
     */
    data object Post : StartGameEvent()

}

/**
 * This event is called when the game shuts down. You might want to avoid saving
 * settings/data **only** on this event, as it might lead to possible data loss.
 */
object ShutdownEvent : Event()

/**
 * Event call in the event of loading, or unloading a world.
 *
 * Split into [Load] and [Unload].
 */
sealed class WorldEvent(val world: World) : Event() {

    /**
     * Called on the event of a world loading, i.e. a server connection, or a singleplayer world.
     */
    class Load(world: World) : WorldEvent(world)

    /**
     * Called on the event of a world unloading, i.e. a server disconnect, or leaving a singleplayer world.
     */
    class Unload(world: World) : WorldEvent(world)
}

/**
 * This cancellable event is called when packets are being handled by
 * the [Network Manager][Minecraft.myNetworkManager].
 *
 * This event is split into [Send] and [Receive].
 *
 * @property packet The packet being processed.
 */
sealed class PacketEvent(val packet: Packet<*>) : CancellableEvent() {

    /**
     * This is called when a packet is being sent by the client, to the server.
     *
     * If cancelled, the packet is not sent.
     */
    class Send(packet: Packet<*>) : PacketEvent(packet)

    /**
     * This is called when a packet is being received by the client, from the server.
     *
     * If cancelled, the packet will not be processed, but instead ignored.
     */
    class Receive(packet: Packet<*>) : PacketEvent(packet)
}
