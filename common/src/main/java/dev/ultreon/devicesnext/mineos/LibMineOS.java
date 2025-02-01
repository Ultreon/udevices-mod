package dev.ultreon.devicesnext.mineos;

import java.io.IOException;
import java.nio.ByteBuffer;

public class LibMineOS implements SystemLibrary {
    private final OperatingSystemImpl operatingSystem;

    LibMineOS(OperatingSystemImpl operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    void _init() {
        // TODO
    }

    void _destroy() {
        // TODO
    }

    void _tick() {
        // TODO
    }

    AppConfig getAppConfig(ApplicationId id) {
        return operatingSystem.getAppConfig(id);
    }

    public String readModule(String resourceName) {
        resourceName = resourceName.startsWith("@ultreon/mineos/system/")
                ? resourceName.replace("@ultreon/mineos/system/", "/system/bin/")
                : "/apps/" + resourceName;

        LibStd fsNode = operatingSystem.getStdLib();
        int fd = fsNode.open(resourceName, 0);

        if (fd == -1) {
            return null;
        }

        try {
            long l = fsNode.fstat(fd).st_size();
            if (l > Integer.MAX_VALUE) throw new IOException("File too large");
            ByteBuffer buffer = ByteBuffer.allocate((int) l);
            fsNode.read(fd, buffer);
            buffer.flip();
            return new String(buffer.array());
        } catch (IOException e) {
            throw new FileSystemIoException(e);
        }
    }
}
