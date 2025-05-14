package dev.ultreon.devicesnext.device;

import dev.ultreon.devicesnext.device.hardware.Drive;
import dev.ultreon.devicesnext.device.hardware.MCHardDrive;
import dev.ultreon.mods.xinexlib.event.server.ServerStoppedEvent;
import dev.ultreon.mods.xinexlib.event.system.EventSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DriveManager {
    private static DriveManager instance;

    static {
        EventSystem.MAIN.on(ServerStoppedEvent.class, DriveManager::unload);
    }

    private static void unload(ServerStoppedEvent event) {
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
