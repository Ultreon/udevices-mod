package com.ultreon.mods.lib.client.devicetest.sizing;

import java.util.Objects;

public class LongSize extends AbstractSize {
    public long width;
    public long height;

    public LongSize(long width, long height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public Long getWidth() {
        return width;
    }

    @Override
    public Long getHeight() {
        return height;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var longSize = (LongSize) o;
        return longSize.width == width && longSize.height == height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }
}
