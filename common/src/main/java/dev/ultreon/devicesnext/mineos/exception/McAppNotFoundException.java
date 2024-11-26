package dev.ultreon.devicesnext.mineos.exception;

import dev.ultreon.devicesnext.mineos.ApplicationId;

public class McAppNotFoundException extends McOperatingSystemException {
    public McAppNotFoundException(ApplicationId id) {
        super("Application not found: " + id);
    }
}
