package dev.ultreon.devicesnext.api;

import dev.ultreon.devicesnext.network.packets.GfxCallPacket;
import dev.ultreon.devicesnext.network.packets.GfxCallbackPacket;

import java.util.UUID;

public interface ConnectedClient {

    String getName();

    UUID getUuid();

    void sendPacket(GfxCallPacket gfxCallbackPacket);
}
