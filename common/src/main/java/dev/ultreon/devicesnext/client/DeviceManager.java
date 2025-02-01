package dev.ultreon.devicesnext.client;

import dev.architectury.utils.EnvExecutor;
import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.device.McDevice;
import dev.ultreon.devicesnext.server.ServerDeviceManager;
import net.minecraft.world.level.Level;

import java.util.UUID;

public interface DeviceManager {

    static DeviceManager get(Level level) {
        return EnvExecutor.getEnvSpecific(
                () -> {
                    if (level.isClientSide()) return ClientDeviceManager::get;
                    return ServerDeviceManager::get;
                },
                () -> ServerDeviceManager::get
        );
    }

    void registerDevice(McDevice device, DeviceBlockEntity blockEntity);

    void unregisterDevice(McDevice device, DeviceBlockEntity blockEntity);

    void unregisterDevice(UUID uuid);

    McDevice getDevice(UUID uuid);

    DeviceBlockEntity getBlock(McDevice device);

    default DeviceBlockEntity getBlock(UUID uuid) {
        McDevice device = getDevice(uuid);
        if (device == null) return null;
        return getBlock(device);
    }
}
