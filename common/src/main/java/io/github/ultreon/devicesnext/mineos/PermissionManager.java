package com.ultreon.mods.lib.client.devicetest;

import com.ultreon.mods.lib.client.devicetest.exception.McNoPermissionException;
import com.ultreon.mods.lib.client.devicetest.security.Permission;

import java.util.HashMap;
import java.util.Map;

public class PermissionManager {
    private final Map<ApplicationId, PermissionContainer> permissions = new HashMap<>();

    public PermissionManager() {

    }

    void grantPermission(ApplicationId applicationId, Permission permission) {
        this.permissions.get(applicationId).grant(permission);
    }

    void registerPerms(ApplicationId id) {
        this.permissions.putIfAbsent(id, new PermissionContainer());
    }

    public boolean hasPermission(Application executor, Permission permission) {
        if (permission == null) return false;
        if (executor instanceof Kernel) return true;
        return this.permissions.get(executor.getId()).has(permission);
    }

    public void dispose() {
        this.permissions.values().forEach(PermissionContainer::dispose);
        this.permissions.clear();
    }

    public void checkPermission(Application executor, Permission permission) throws McNoPermissionException {
        if (permission != null && this.hasPermission(executor, permission)) return;

        throw new McNoPermissionException(executor, permission);
    }
}
