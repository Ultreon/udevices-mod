package io.github.ultreon.devicesnext.fabric;

import io.github.ultreon.devicesnext.Udevices;
import net.fabricmc.api.ModInitializer;

public class UdevicesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Udevices.init();
    }
}