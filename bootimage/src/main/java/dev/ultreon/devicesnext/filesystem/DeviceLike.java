package dev.ultreon.devicesnext.filesystem;

import org.jnode.driver.DeviceAPI;

public interface DeviceLike {

    String getId();

    <T extends DeviceAPI> void registerAPI(Class<T> apiInterface, T apiImplementation);
}
