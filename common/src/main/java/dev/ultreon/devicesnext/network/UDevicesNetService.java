package dev.ultreon.devicesnext.network;

import com.ultreon.mods.lib.network.api.service.NetworkService;

public class UDevicesNetService implements NetworkService {
    @Override
    public void setup() {
        UDevicesNet.setup();
    }
}
