package io.github.ultreon.devicesnext.mineos;

import io.github.ultreon.devicesnext.mineos.security.Permission;

import java.util.HashSet;
import java.util.Set;

class PermissionContainer {
    private final Set<Permission> permissions = new HashSet<>();

    PermissionContainer() {

    }

    void dispose() {
        this.permissions.clear();
    }

    void grant(Permission permission) {
        this.permissions.add(permission);
    }

    void revoke(Permission permission) {
        this.permissions.add(permission);
    }

    public boolean has(Permission permission) {
        return this.permissions.contains(permission);
    }
}
