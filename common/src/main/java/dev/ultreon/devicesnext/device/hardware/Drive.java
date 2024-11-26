package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.fs.FS;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.device.VirtualBlockDevice;
import dev.ultreon.devicesnext.device.McDevice;
import org.jnode.driver.Device;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public abstract class Drive extends VirtualBlockDevice implements HardwareComponent {
    private final McDevice device;
    @org.jetbrains.annotations.NotNull
    protected final Drive.Class type;
    private final UUID id;

    public Drive(McDevice device, UUID id, Class type) throws IOException {
        super(UDevicesMod.getDataPath().resolve("vdisks/" + device.getId() + "/hd_" + id.toString().replace("-", "") + type.name().toLowerCase() + ".img").toString(), type.bytes);
        this.device = device;
        this.id = id;
        this.type = type;

        device.registerAPI(Drive.class, this);
    }

    public static boolean isTaken(McDevice device, UUID uuid, Class type) {
        return Files.exists(UDevicesMod.getDataPath().resolve("vdisks/" + device.getId() + "/hd_" + uuid.toString().replace("-", "") + type.name().toLowerCase() + ".img"));
    }

    public Device getDevice() {
        return device;
    }

    public abstract FS getFileSystem();

    public abstract void format() throws IOException;

    public abstract boolean isUninitialized();

    @Override
    public UUID getUuid() {
        return id;
    }

    public enum Class {
        SMALL(64 * 1024),
        MEDIUM(1024 * 1024),
        LARGE(16 * 1024 * 1024);

        public final int bytes;

        Class(int bytes) {
            this.bytes = bytes;
        }
    }
}
