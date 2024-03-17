package com.ultreon.mods.lib.client.devicetest.security;

import com.ultreon.mods.lib.client.devicetest.ApplicationId;
import com.ultreon.mods.lib.client.devicetest.DesktopApplication;

import java.util.Objects;

public class SpawnApplicationPermission extends Permission {
    private final ApplicationId id;

    public SpawnApplicationPermission(DesktopApplication application) {
        this(application.getId());
    }

    public SpawnApplicationPermission(ApplicationId id) {
        super("spawn_application");
        this.id = id;
    }

    @Override
    public String toString() {
        return "McSpawnApplicationPermission{" +
                "id=" + id +
                ", name='" + getName() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpawnApplicationPermission that = (SpawnApplicationPermission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
