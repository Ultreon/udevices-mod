package dev.ultreon.devicesnext.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.device.McDevice;
import dev.ultreon.devicesnext.server.ServerGPU;
import dev.ultreon.devicesnext.mineos.DeviceScreen;
import dev.ultreon.devicesnext.util.Arguments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jnode.driver.ApiNotFoundException;

public class UDevicesModClient {
    public static void init() {
        ClientTickEvent.CLIENT_LEVEL_PRE.register(instance -> {
            if (Minecraft.getInstance().screen instanceof DeviceScreen) {
                return;
            }
        });
    }

    public static void onGfxCall(BlockPos pos, int ptr, Arguments args) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DeviceBlockEntity deviceBlockEntity)) return;
        McDevice device = deviceBlockEntity.getDevice();
        if (!device.implementsAPI(ServerGPU.class)) return;

        device.getUuid();
    }
}
