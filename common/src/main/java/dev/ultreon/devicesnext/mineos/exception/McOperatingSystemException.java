package dev.ultreon.devicesnext.mineos.exception;

public class McOperatingSystemException extends Exception {
    public McOperatingSystemException() {
        
    }

    public McOperatingSystemException(String message) {
        super(message);
    }

    public McOperatingSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public McOperatingSystemException(Throwable cause) {
        super(cause);
    }
}
