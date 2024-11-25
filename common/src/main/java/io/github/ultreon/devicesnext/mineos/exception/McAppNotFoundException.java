package io.github.ultreon.devicesnext.mineos.exception;

import io.github.ultreon.devicesnext.mineos.ApplicationId;

public class McAppNotFoundException extends McOperatingSystemException {
    public McAppNotFoundException(ApplicationId id) {
        super("Application not found: " + id);
    }
}
