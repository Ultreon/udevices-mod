package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSFile;

import java.nio.ByteBuffer;
import java.util.Objects;

import static dev.ultreon.devicesnext.mineos.LibStd.O_RDONLY;
import static dev.ultreon.devicesnext.mineos.LibStd.O_WRONLY;

public final class FileDescriptor {
    private final int fd;
    private String path;
    private FSFile file;
    private long off;
    private int flags;

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

    public void open(String path, FSFile file, int flags) {
        this.path = path;
        this.file = file;
        this.off = 0;
        this.flags = flags;
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
        if ((flags & O_WRONLY) == O_WRONLY) {
            throw new FileSystemIoException("File is write-only");
        }
        file.read(off, buffer);
    }

    public void write(ByteBuffer buffer) {
        if ((flags & O_RDONLY) == O_RDONLY) {
            throw new FileSystemIoException("File is read-only");
        }

        file.write(off, buffer);
    }

    public long tell() {
        return off;
    }

    public int getFlags() {
        return flags;
    }

    public void truncate(long size) {
        file.setLength(size);
    }

    public void seek(long pos) {
        off = pos;
    }
}
