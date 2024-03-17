package io.github.ultreon.devicesnext.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.ultreon.devicesnext.Udevices;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Udevices.MOD_ID)
public class UdevicesForge {
    public UdevicesForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Udevices.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Udevices.init();
    }
}