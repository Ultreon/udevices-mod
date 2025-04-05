package dev.ultreon.devicesnext.filesystem;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PathHandle implements Iterable<String
        > {
    public String path;

    public PathHandle(String path) {
        if (!path.startsWith("/"))
            throw new IllegalArgumentException("Path must start with /");
        this.path = path;
    }

    public PathHandle child(String name) {
        if (path.equals("/")) return new PathHandle("/" + name);
        return new PathHandle(path + "/" + name);
    }

    public PathHandle getParent() {
        if (path.equals("/")) return null;
        if (path.lastIndexOf("/") == 0) return new PathHandle("/");
        return new PathHandle(path.substring(0, path.lastIndexOf("/")));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PathHandle) obj;
        return this.path.equals(that.path);
    }

    public String getName() {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public boolean isRoot() {
        return path.equals("/");
    }

    public String getExtension() {
        return getName().substring(path.lastIndexOf(".") + 1);
    }

    public String getNameWithoutExtension() {
        if (getName().lastIndexOf(".") == 0) return getName();
        return getName().substring(0, path.lastIndexOf("."));
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public @NotNull Iterator<String> iterator() {
        return new Iterator<>() {
            private final String[] split = path.substring(1).split("/");
            private int index = 0;

            @Override
            public boolean hasNext() {
                return !path.equals("/") && index < split.length;
            }

            @Override
            public String next() {
                if (!hasNext()) throw new NoSuchElementException();
                return split[index++];
            }
        };
    }
}
