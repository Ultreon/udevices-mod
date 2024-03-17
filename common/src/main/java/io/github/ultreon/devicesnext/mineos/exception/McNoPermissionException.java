package com.ultreon.mods.lib.client.devicetest.exception;

import com.ultreon.mods.lib.client.devicetest.Application;
import com.ultreon.mods.lib.client.devicetest.security.Permission;

public class McNoPermissionException extends McPermissionException {

    public McNoPermissionException(Application executor, Permission permission) {
        super("App " + executor.getId() + " has no permission: " + permission, permission);
    }

    public McNoPermissionException(Application executor, Permission permission, Throwable cause) {
        super("App " + executor.getId() + " has no permission: " + permission, cause, permission);
    }
}
