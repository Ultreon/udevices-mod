package com.ultreon.mods.lib.client.devicetest.security;

import java.util.Objects;

public class Permission {
    public static final Permission FILE_SYSTEM = new Permission("file_system");
    public static final Permission SPAWN_APPLICATIONS = new Permission("spawn_applications");
    public static final Permission HARD_ERROR = new Permission("hard_error");
    public static final Permission SHUTDOWN = new Permission("shutdown");
    public static final Permission LIST_APPLICATIONS = new Permission("list_applications");

    private final String name;

    public Permission(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "McPermission{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
