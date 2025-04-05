package dev.ultreon.devicesnext.device;

import dev.ultreon.mods.xinexlib.event.server.ServerStoppingEvent;
import dev.ultreon.mods.xinexlib.event.system.EventSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DriveManager {
    private static DriveManager instance;

    static {
        EventSystem.MAIN.on(ServerStoppingEvent.class, DriveManager::unload);
    }

    private static void unload(ServerStoppingEvent server) {
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

    private static DriveManager load(MinecraftServer currentServer) throws IOException {
        Path worldPath = currentServer.getWorldPath(LevelResource.ROOT);
        Path vdisks = worldPath.resolve("vdisks");
        if (Files.notExists(vdisks)) Files.createDirectories(vdisks);
        return instance = new DriveManager();
    }
}
