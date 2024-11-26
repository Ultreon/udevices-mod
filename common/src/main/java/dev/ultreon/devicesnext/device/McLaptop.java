package dev.ultreon.devicesnext.device;

import java.util.UUID;

public class McLaptop extends McDevice {
    protected McLaptop(String id) {
        super(id, UUID.randomUUID());
    }
}
