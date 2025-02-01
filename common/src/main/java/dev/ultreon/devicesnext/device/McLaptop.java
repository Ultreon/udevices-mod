package dev.ultreon.devicesnext.device;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class McLaptop extends McDevice {
    protected McLaptop(String id) {
        super(id, UUID.randomUUID());
    }

    @Override
    public void open(@NotNull Player player) {

    }

    @Override
    public void close(@NotNull Player player) {

    }
}
