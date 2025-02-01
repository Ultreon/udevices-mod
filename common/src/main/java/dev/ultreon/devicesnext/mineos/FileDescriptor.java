package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSFile;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class FileDescriptor {
    private final int fd;
    private String path;
    private FSFile file;
    private long off;

    public FileDescriptor(
            int fd,
            String path,
            FSFile file
    ) {
        this.fd = fd;
        this.path = path;
        this.file = file;
    }

    public int fd() {
        return fd;
    }

    public String getPath() {
        return path;
    }

    public FSFile getFile() {
        return file;
    }

    public void open(String path, FSFile file) {
        this.path = path;
        this.file = file;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FileDescriptor) obj;
        return this.fd == that.fd &&
               Objects.equals(this.path, that.path) &&
               Objects.equals(this.file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fd, path, file);
    }

    @Override
    public String toString() {
        return "FileDescriptor[" +
               "fd=" + fd + ", " +
               "path=" + path + ", " +
               "file=" + file + ']';
    }

    public void read(ByteBuffer buffer) {
        file.read(off, buffer);
    }

    public void write(ByteBuffer buffer) {
        file.write(off, buffer);
    }
}
