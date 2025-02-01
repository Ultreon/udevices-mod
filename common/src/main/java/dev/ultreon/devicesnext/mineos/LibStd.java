package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSFile;
import dev.ultreon.devicesnext.device.hardware.FSNode;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class LibStd implements SystemLibrary {
    private final OperatingSystemImpl operatingSystem;
    private final FileDescriptorManager fdManager;
    private String error = null;
    private int errno = 0;

    LibStd(OperatingSystemImpl operatingSystem) {
        this.operatingSystem = operatingSystem;
        this.fdManager = new FileDescriptorManager(operatingSystem);
    }

    void _init() {
        // TODO
    }

    void _destroy() {
        // TODO
    }

    void _tick() {
        // TODO
    }

    int open(String path, int flags) {
        FSNode fsNode = operatingSystem.getFileSystem().get(Path.of(path));
        if (fsNode == null) {
            this.error = "File not found: " + path;
            this.errno = 2;
            return -1;
        }
        if (!(fsNode instanceof FSFile fsFile)) {
            this.error = "Not a file: " + path;
            this.errno = 2;
            return -1;
        }
        return operatingSystem.gerFdManager().open(path, fsFile);
    }

    void close(int fd) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = 9;
            return;
        }
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        if (fileDescriptor == null) {
            return;
        }
        operatingSystem.gerFdManager().free(fileDescriptor);
    }

    int read(int fd, ByteBuffer buffer) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = 9;
            return -1;
        }

        buffer.flip();
        buffer.position(0);
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        fileDescriptor.read(buffer);
        return buffer.position();
    }

    int write(int fd, ByteBuffer buffer) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = 9;
            return -1;
        }

        buffer.flip();
        buffer.position(0);
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        fileDescriptor.write(buffer);
        return buffer.position();
    }

    FStat fstat(int fd) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = 9;
            return null;
        }
        FSFile file = operatingSystem.gerFdManager().get(fd).getFile();
        return new FStat(file.getLength(), file.getMode(), file.getLastAccessed(), file.getLastModified(), file.getCreated());
    }

    int ftruncate(int fd, long size) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = 9;
            return -1;
        }

        // TODO
        return -1;
    }

    int unlink(String path) {
        // TODO
        return -1;
    }

    int mkdir(String path, int mode) {
        // TODO
        return -1;
    }

    int rmdir(String path) {
        // TODO
        return -1;
    }

    int chdir(String path) {
        // TODO
        return -1;
    }

    String getcwd() {
        // TODO
        return null;
    }

    public record FStat(long st_size, long st_mode, long st_atime, long st_mtime, long st_ctime) {

    }
}
