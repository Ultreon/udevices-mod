package dev.ultreon.devicesnext.neoforge;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.mods.xinexlib.platform.NeoForgePlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(UDevicesMod.MOD_ID)
public class UDevicesModNeoForge {
    public UDevicesModNeoForge(IEventBus modEventBus) {
		// Submit our event bus to let architectury register our content on the right time
        NeoForgePlatform.getPlatform().registerMod(UDevicesMod.MOD_ID, modEventBus);
        UDevicesMod.init();
    }
}