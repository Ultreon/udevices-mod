package com.ultreon.mods.lib.client.devicetest;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

public class ShutdownToken {
    ScheduledFuture<ShutdownToken> schedule;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShutdownToken that = (ShutdownToken) o;
        return Objects.equals(schedule, that.schedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schedule);
    }

    @Override
    public String toString() {
        return "ShutdownToken(" + hashCode() + ")";
    }
}
