package com.ultreon.mods.lib.client.devicetest.exception;

public class McAccessDeniedException extends McSecurityException {
    public McAccessDeniedException() {
    }

    public McAccessDeniedException(String message) {
        super(message);
    }

    public McAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public McAccessDeniedException(Throwable cause) {
        super(cause);
    }
}
