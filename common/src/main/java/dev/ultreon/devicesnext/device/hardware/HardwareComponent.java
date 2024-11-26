package dev.ultreon.devicesnext.device.hardware;

import org.jnode.driver.DeviceAPI;

import java.io.Closeable;
import java.util.UUID;

public interface HardwareComponent extends DeviceAPI, Closeable {
    UUID getUuid();
}
