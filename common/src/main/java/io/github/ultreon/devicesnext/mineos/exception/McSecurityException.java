package io.github.ultreon.devicesnext.mineos.exception;

public class McSecurityException extends McOperatingSystemException {
    public McSecurityException() {
    }

    public McSecurityException(String message) {
        super(message);
    }

    public McSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public McSecurityException(Throwable cause) {
        super(cause);
    }
}
