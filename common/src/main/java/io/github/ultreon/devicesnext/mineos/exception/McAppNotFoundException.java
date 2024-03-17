package com.ultreon.mods.lib.client.devicetest.exception;

import com.ultreon.mods.lib.client.devicetest.ApplicationId;

public class McAppNotFoundException extends McOperatingSystemException {
    public McAppNotFoundException(ApplicationId id) {
        super("Application not found: " + id);
    }
}
