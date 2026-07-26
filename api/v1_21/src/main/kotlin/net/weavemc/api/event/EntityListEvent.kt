package net.weavemc.api.event

import net.minecraft.entity.Entity

/**
 * This event is called when an entity is added to or removed from the world.
 *
 * It is split into [EntityListEvent.Add] and [EntityListEvent.Remove].
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
    public class Remove(entity: Entity, public val reason: Entity.RemovalReason) : EntityListEvent(entity)
}