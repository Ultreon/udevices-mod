package io.github.ultreon.devicesnext.mineos.exception;

import io.github.ultreon.devicesnext.mineos.security.Permission;

public class McPermissionException extends McSecurityException {
    private final Permission permission;

    public McPermissionException(String message, Permission permission) {
        super(message);
        this.permission = permission;
    }

    public McPermissionException(String message, Throwable cause, Permission permission) {
        super(message, cause);
        this.permission = permission;
    }

    public Permission getPermission() {
        return this.permission;
    }
}
