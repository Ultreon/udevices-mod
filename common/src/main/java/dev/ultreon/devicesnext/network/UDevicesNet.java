package dev.ultreon.devicesnext.network;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.network.packets.GfxCallPacket;
import dev.ultreon.mods.xinexlib.network.NetworkRegistry;
import dev.ultreon.mods.xinexlib.network.Networker;
import dev.ultreon.mods.xinexlib.platform.XinexPlatform;

public class UDevicesNet {
    private static final Networker INSTANCE = XinexPlatform.createNetworker(UDevicesMod.MOD_ID, UDevicesNet::registerPackets);

    private static void registerPackets(NetworkRegistry registry) {
        registry.registerClient("gfx_call", GfxCallPacket.class, GfxCallPacket::read);
    }

    public static Networker get() {
        return INSTANCE;
    }

    public static void setup() {
        // Do nothing
    }
}
