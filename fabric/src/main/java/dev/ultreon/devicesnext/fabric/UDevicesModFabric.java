package dev.ultreon.devicesnext.fabric;

import dev.ultreon.devicesnext.UDevicesMod;
import net.fabricmc.api.ModInitializer;

public class UDevicesModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        UDevicesMod.init();
    }
}