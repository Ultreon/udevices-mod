package dev.ultreon.devicesnext.filesystem;

import org.jnode.driver.DeviceAPI;

import java.io.Closeable;
import java.util.UUID;

public interface HardwareComponent extends DeviceAPI, Closeable {
    UUID getUuid();
}
