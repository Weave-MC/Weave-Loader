@file:JvmName("MinecraftMixin")

package club.maxstats.weave.loader.mixins.core

import club.maxstats.weave.loader.WeaveLoader
import club.maxstats.weave.loader.api.WeavePhase
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Suppress("UNUSED_PARAMETER")
@Mixin(Minecraft::class)
public class MinecraftMixin {
    @Inject(at = [At("HEAD")], method = ["startGame"])
    public fun preStartGame(ci: CallbackInfo) {
        WeaveLoader.switchPhase(WeavePhase.INIT)
    }

    @Inject(at = [At("TAIL")], method = ["startGame"])
    public fun postStartGame(ci: CallbackInfo) {
        WeaveLoader.initLegacyMods()
        WeaveLoader.switchPhase(WeavePhase.POST_INIT)
    }
}
