package io.github.ultreon.devicesnext.fabric;

import io.github.ultreon.devicesnext.UDevicesMod;
import net.fabricmc.api.ModInitializer;

public class UDevicesModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        UDevicesMod.init();
    }
}