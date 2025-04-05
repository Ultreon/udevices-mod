package dev.ultreon.devicesnext.server;

import dev.ultreon.devicesnext.block.entity.DeviceBlockEntity;
import dev.ultreon.devicesnext.client.DeviceManager;
import dev.ultreon.devicesnext.device.McDevice;
import dev.ultreon.mods.xinexlib.event.server.ServerStartedEvent;
import dev.ultreon.mods.xinexlib.event.server.ServerStoppingEvent;
import dev.ultreon.mods.xinexlib.event.system.EventSystem;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerDeviceManager implements DeviceManager {
    private static ServerDeviceManager instance;

    static {
        EventSystem.MAIN.on(ServerStartedEvent.class, ServerDeviceManager::create);
        EventSystem.MAIN.on(ServerStoppingEvent.class, ServerDeviceManager::unload);
    }

    private final Map<UUID, McDevice> devices = new HashMap<>();
    private final Map<McDevice, DeviceBlockEntity> blocks = new HashMap<>();
    private final MinecraftServer server;

    private ServerDeviceManager(MinecraftServer server) {
        this.server = server;
    }

    private static void create(ServerStartedEvent server) {
        instance = new ServerDeviceManager(server.getServer());
    }

    private static void unload(ServerStoppingEvent server) {
        if (server.getServer() == instance.server) {
            instance = null;
        }
    }

    public static ServerDeviceManager get() {
        return ServerDeviceManager.instance;
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

    public MinecraftServer getServer() {
        return server;
    }

    public Collection<McDevice> getDevices() {
        return devices.values();
    }
}
