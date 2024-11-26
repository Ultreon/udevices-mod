package dev.ultreon.devicesnext.device;

import dev.ultreon.devicesnext.device.hardware.HardwareComponent;
import org.jnode.driver.virtual.VirtualDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class McDevice extends VirtualDevice {
    private final List<HardwareComponent> components = new ArrayList<>();
    private final UUID uuid;

    protected McDevice(String id, UUID uuid) {
        super(id);
        this.uuid = uuid;
    }

    protected void registerComponent(HardwareComponent component) {
        this.components.add(component);
    }

    public List<HardwareComponent> getComponents() {
        return this.components;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void close() throws IOException {
        for (HardwareComponent c : this.components) {
            c.close();
        }
    }
}
