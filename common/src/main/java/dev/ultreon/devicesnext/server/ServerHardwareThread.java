package dev.ultreon.devicesnext.server;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.cpu.CPU;
import dev.ultreon.devicesnext.gpu.GPU;
import net.minecraft.server.MinecraftServer;
import org.jnode.driver.Device;

public class ServerHardwareThread extends Thread {
    private final MinecraftServer server;

    public ServerHardwareThread(MinecraftServer server) {
        super("ServerHardwareThread");
        this.server = server;
    }

    @Override
    public void run() {
        while (server.isRunning()) {
            try {
                Device[] devices = ServerDeviceManager.get().getDevices().toArray(new Device[0]);
                for (Device d : devices) {
                    if (d instanceof CPU) {
                        ((CPU) d).tick();
                    } else if (d instanceof GPU) {
                        ((GPU) d).tick();
                    }
                }
            } catch (Exception e) {
                UDevicesMod.LOGGER.error("Failed to tick hardware", e);
            }
        }
    }
}
