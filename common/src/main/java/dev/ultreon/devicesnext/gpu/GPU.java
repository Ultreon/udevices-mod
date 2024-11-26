package dev.ultreon.devicesnext.gpu;

import dev.ultreon.devicesnext.util.FuncObject;
import org.jnode.driver.DeviceAPI;

public interface GPU extends FuncObject, DeviceAPI {
    void tick();
}
