package dev.ultreon.devicesnext.client;

import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.device.McDevice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientDeviceManager implements DeviceManager {
    private static final DeviceManager INSTANCE = new ClientDeviceManager();
    private final Map<UUID, McDevice> devices = new HashMap<>();
    private final Map<McDevice, DeviceBlockEntity> blocks = new HashMap<>();

    private ClientDeviceManager() {

    }

    public static DeviceManager get() {
        return ClientDeviceManager.INSTANCE;
    }

    @Override
    public void registerDevice(McDevice device, DeviceBlockEntity blockEntity) {
        devices.put(device.getUuid(), device);
        blocks.put(device, blockEntity);
    }

    @Override
    public void unregisterDevice(McDevice device, DeviceBlockEntity blockEntity) {
        McDevice remove = devices.remove(device.getUuid());
        if (remove == null) return;
        blocks.remove(remove);
    }

    @Override
    public void unregisterDevice(UUID uuid) {
        McDevice remove = devices.remove(uuid);
        if (remove == null) return;
        blocks.remove(remove);
    }

    public void clear() throws IOException {
        for (McDevice mcDevice : devices.values()) {
            mcDevice.close();
        }
        devices.clear();
        blocks.clear();
    }

    @Override
    public McDevice getDevice(UUID uuid) {
        return devices.get(uuid);
    }

    @Override
    public DeviceBlockEntity getBlock(McDevice device) {
        return blocks.get(device);
    }
}
