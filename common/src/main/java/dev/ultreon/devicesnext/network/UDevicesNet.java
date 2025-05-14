package dev.ultreon.devicesnext.network;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.network.packets.GfxCallPacket;
import dev.ultreon.devicesnext.network.packets.GfxCallbackPacket;
import dev.ultreon.mods.xinexlib.network.Networker;
import dev.ultreon.mods.xinexlib.platform.XinexPlatform;

public class UDevicesNet {
    private static final Networker instance = XinexPlatform.createNetworker(UDevicesMod.MOD_ID, networkRegistry -> {
        networkRegistry.registerClient("gfx_call", GfxCallPacket.class, GfxCallPacket::read);
        networkRegistry.registerServer("gfx_callback", GfxCallbackPacket.class, GfxCallbackPacket::read);
    });

    public static Networker get() {
        return instance;
    }

    public static void setup() {
        // Do nothing
    }
}
