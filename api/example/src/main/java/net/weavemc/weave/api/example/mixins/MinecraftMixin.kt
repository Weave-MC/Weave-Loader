package net.weavemc.weave.api.example.mixins

import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Minecraft::class)
class MinecraftMixin {
    @Inject(method = ["startGame"], at = [At("HEAD")])
    fun onStartGame(ci: CallbackInfo) {
        println("ExampleMod Minecraft#startGame Mixin Test")
    }
}
