package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSFile;
import dev.ultreon.devicesnext.device.hardware.FSNode;

import java.nio.ByteBuffer;
import java.nio.file.Path;

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
}
