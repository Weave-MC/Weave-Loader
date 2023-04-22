package net.weavemc.loader.api.event.client

import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.DamageSource
import net.weavemc.loader.api.event.CancellableEvent

public class LivingAttackEvent(
    public val entity: EntityLivingBase,
    public val source: DamageSource,
    public val amount: Float
) : CancellableEvent()
