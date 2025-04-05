package dev.ultreon.devicesnext.filesystem;

@SuppressWarnings("ClassCanBeRecord")
public class LockKey {
    private final PathHandle path;

    public LockKey(PathHandle path) {
        this.path = path;
    }

    public PathHandle getPath() {
        return path;
    }
}
