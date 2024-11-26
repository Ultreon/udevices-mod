package dev.ultreon.devicesnext.network;

import com.ultreon.mods.lib.network.api.Network;
import com.ultreon.mods.lib.network.api.PacketRegisterContext;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.network.packets.GfxCallPacket;

public class UDevicesNet extends Network {
    private static Network instance;

    private UDevicesNet() {
        super(UDevicesMod.MOD_ID, "main");
    }

    public static Network get() {
        return instance;
    }

    public static void setup() {
        instance = new UDevicesNet();
    }

    @Override
    protected void registerPackets(PacketRegisterContext ctx) {
        ctx.register(GfxCallPacket::read);
    }
}
