package net.weavemc.loader.api.event.client

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.world.World
import net.weavemc.loader.api.event.CancellableEvent
import net.weavemc.loader.api.event.Event

public class LivingUpdateEvent(public val entityLiving: EntityLivingBase) : CancellableEvent()

public sealed class WorldEvent(public val world: World) : Event() {
    public class Load(world: World) : WorldEvent(world)

    public class Unload(world: World) : WorldEvent(world)
}

public sealed class EntityListEvent(public val entity: Entity) : Event() {
    public class Add(entity: Entity) : EntityListEvent(entity)

    public class Remove(entity: Entity) : EntityListEvent(entity)
}

public sealed class PlayerListEvent(public val playerData: S38PacketPlayerListItem.AddPlayerData) : Event() {
    public class Add(playerData: S38PacketPlayerListItem.AddPlayerData) : PlayerListEvent(playerData)

    public class Remove(playerData: S38PacketPlayerListItem.AddPlayerData) : PlayerListEvent(playerData)
}

public class EntityJoinWorldEvent(public val entity: Entity, public val world: World) : CancellableEvent()
