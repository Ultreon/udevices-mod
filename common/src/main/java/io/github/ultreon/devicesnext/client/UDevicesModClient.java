package io.github.ultreon.devicesnext.client;

import dev.architectury.event.events.client.ClientTickEvent;
import io.github.ultreon.devicesnext.mineos.DeviceScreen;
import net.minecraft.client.Minecraft;

public class UDevicesModClient {
    public static void init() {
        ClientTickEvent.CLIENT_LEVEL_PRE.register(instance -> {
            if (Minecraft.getInstance().screen instanceof DeviceScreen) {
                return;
            }
        });
    }
}
