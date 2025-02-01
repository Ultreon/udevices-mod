package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSFile;
import dev.ultreon.devicesnext.device.hardware.FSNode;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class LibStd implements SystemLibrary {
    private final OperatingSystemImpl operatingSystem;
    private final FileDescriptorManager fdManager;

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
        if (!(fsNode instanceof FSFile fsFile)) {
            return -1;
        }
        operatingSystem.gerFdManager().open(path, fsFile);
        return -1;
    }

    void close(int fd) {
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        if (fileDescriptor == null) {
            return;
        }
        operatingSystem.gerFdManager().free(fileDescriptor);
    }

    int read(int fd, ByteBuffer buffer) {
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        fileDescriptor.read(buffer);
        return -1;
    }

    int write(int fd, ByteBuffer buffer) {
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        fileDescriptor.write(buffer);
        return -1;
    }

    FStat fstat(int fd) {
        FSFile file = operatingSystem.gerFdManager().get(fd).getFile();
        return new FStat(file.getLength(), file.getMode(), file.getLastAccessed(), file.getLastModified(), file.getCreated());
    }

    int ftruncate(int fd, long size) {
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
