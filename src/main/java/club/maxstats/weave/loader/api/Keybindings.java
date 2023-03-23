package club.maxstats.weave.loader.api;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;

@UtilityClass
public class Keybindings {
    public void registerKeyBinding(KeyBinding key) {
        Minecraft.getMinecraft().gameSettings.keyBindings = ArrayUtils.add(
            Minecraft.getMinecraft().gameSettings.keyBindings,
            key
        );
    }
}
