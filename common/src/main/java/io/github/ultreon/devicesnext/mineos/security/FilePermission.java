package com.ultreon.mods.lib.client.devicetest.security;

import java.util.Objects;

public class FilePermission extends Permission {
    private final String path;

    public FilePermission(String name, String path) {
        super(name);
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return "McFilePermission{" +
                "name='" + getName() + "', " +
                "path='" + path + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FilePermission that = (FilePermission) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path);
    }
}
