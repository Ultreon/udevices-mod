package dev.ultreon.devicesnext.device;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.ultreon.devicesnext.device.hardware.Drive;
import dev.ultreon.devicesnext.device.hardware.MCHardDrive;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DriveManager {
    private static DriveManager instance;

    static {
        LifecycleEvent.SERVER_STOPPED.register(DriveManager::unload);
    }

    private static void unload(MinecraftServer server) {
        instance = null;
    }

    private DriveManager() {

    }

    public static DriveManager get(MinecraftServer server) {
        if (server == null) return null;

        DriveManager manager = instance;
        if (manager == null) {
            try {
                return load(server);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return manager;
    }

    public Drive create(McDevice device, Drive.Class type) throws IOException {
        UUID uuid = UUID.randomUUID();
        while (isIdTaken(device, uuid, type)) {
            uuid = UUID.randomUUID();
        }
        return new MCHardDrive(device, uuid);
    }

    private boolean isIdTaken(McDevice device, UUID uuid, Drive.Class type) {
        try {
            return Drive.isTaken(device, uuid, type);
        } catch (Exception e) {
            return false;
        }
    }

    private static DriveManager load(MinecraftServer currentServer) throws IOException {
        Path worldPath = currentServer.getWorldPath(LevelResource.ROOT);
        Path vdisks = worldPath.resolve("vdisks");
        if (Files.notExists(vdisks)) Files.createDirectories(vdisks);
        return instance = new DriveManager();
    }
}
