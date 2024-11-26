package dev.ultreon.devicesnext.impl;

import dev.ultreon.devicesnext.api.ConnectedClient;
import dev.ultreon.devicesnext.network.packets.GfxCallPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ConnectedClientImpl implements ConnectedClient {
    private final ServerPlayer player;

    public ConnectedClientImpl(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public String getName() {
        return player.getName().getString();
    }

    @Override
    public UUID getUuid() {
        return player.getUUID();
    }

    @Override
    public void sendPacket(GfxCallPacket gfxCallbackPacket) {
        // Meow :3
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
