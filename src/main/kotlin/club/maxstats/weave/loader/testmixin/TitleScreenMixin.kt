package club.maxstats.weave.loader.testmixin

import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(net.minecraft.client.gui.GuiMainMenu::class)
public class TitleScreenMixin {
    @Inject(at = [At("TAIL")], method = ["<init>()V"])
    public fun init(callback: CallbackInfo) {
        println("This line is printed by an example mod mixin!")
    }
}