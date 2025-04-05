package dev.ultreon.devicesnext.filesystem;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public abstract class Drive extends dev.ultreon.devicesnext.filesystem.VirtualBlockDevice implements HardwareComponent {
    private final DeviceLike device;
    @org.jetbrains.annotations.NotNull
    protected final Drive.Class type;
    private final UUID id;

    public Drive(DeviceLike device, UUID id, Class type) throws IOException {
        super(getPath(device, id, type), type.bytes);
        this.device = device;
        this.id = id;
        this.type = type;

        device.registerAPI(Drive.class, this);
    }

    private static @NotNull String getPath(DeviceLike device, UUID id, Class type) {
        return Constants.DATA_REF.get().resolve("vdisks/" + device.getId() + "/hd_" + id.toString().replace("-", "") + type.name().toLowerCase() + ".img").toString();
    }

    public static boolean isTaken(DeviceLike device, UUID uuid, Class type) {
        return Files.exists(Constants.DATA_REF.get().resolve("vdisks/" + device.getId() + "/hd_" + uuid.toString().replace("-", "") + type.name().toLowerCase() + ".img"));
    }

    public DeviceLike getDevice() {
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
