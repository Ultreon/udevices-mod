package dev.ultreon.devicesnext.mineos.exception;

import dev.ultreon.devicesnext.mineos.Application;
import dev.ultreon.devicesnext.mineos.security.Permission;

public class McNoPermissionException extends McPermissionException {

    public McNoPermissionException(Application executor, Permission permission) {
        super("App " + executor.getId() + " has no permission: " + permission, permission);
    }

    public McNoPermissionException(Application executor, Permission permission, Throwable cause) {
        super("App " + executor.getId() + " has no permission: " + permission, cause, permission);
    }
}
