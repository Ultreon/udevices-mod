package dev.ultreon.devicesnext.client;

import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.device.McDevice;

import java.util.UUID;

public interface DeviceManager {

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
