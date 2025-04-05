package dev.ultreon.devicesnext.device;

import dev.ultreon.devicesnext.server.ServerGPU;
import dev.ultreon.devicesnext.block.entity.LaptopBlockEntity;
import dev.ultreon.devicesnext.filesystem.HardwareComponent;
import org.jnode.driver.DeviceAPI;

import java.io.IOException;
import java.util.UUID;

public class GraphicsDevice implements DeviceAPI, HardwareComponent {
    private final ServerGPU graphicsServer;
    private UUID uuid;

    public GraphicsDevice(LaptopBlockEntity laptop, UUID uuid) {
        this.graphicsServer = new ServerGPU(laptop);
        this.uuid = uuid;
    }

    public ServerGPU getGraphicsServer() {
        return graphicsServer;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void close() throws IOException {
        graphicsServer.close();
    }
}
