package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSDirectory;
import dev.ultreon.devicesnext.device.hardware.FSFile;
import dev.ultreon.devicesnext.device.hardware.FSNode;
import dev.ultreon.devicesnext.device.hardware.FSRoot;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class LibStd implements SystemLibrary {
    private final OperatingSystemImpl operatingSystem;
    private final FileDescriptorManager fdManager;
    public static final int O_CREAT = 0x4000;
    public static final int O_RDONLY = 0x0002;
    public static final int O_WRONLY = 0x0001;
    public static final int O_TRUNC = 0x2000;
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
        if ((flags & O_CREAT) == O_CREAT) {
            String parent = parent(path);
            FSNode fsNode = operatingSystem.getFileSystem().get(parent);
            if (fsNode == null) {
                this.error = "Directory not found: " + parent;
                this.errno = 2;
                return -1;
            }

            fsNode.open();
            if (!(fsNode instanceof FSDirectory fsDirectory)) {
                this.error = "Not a directory: " + parent;
                this.errno = 2;
                return -1;
            }

            fsDirectory.open();
            fsDirectory.createFile(fname(path));
            fsDirectory.close();
        }

        FSNode fsNode = operatingSystem.getFileSystem().get(path);
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
        return operatingSystem.gerFdManager().open(path, fsFile, flags);
    }

    private String fname(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static @NotNull String parent(String path) {
        if (path.equals("/")) {
            return path;
        }
        int endIndex = path.lastIndexOf("/");
        if (endIndex == 0 && path.startsWith("/")) {
            return "/";
        }
        return path.substring(0, endIndex);
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

        fileDescriptor.getFile().flush();
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
        fileDescriptor.truncate(fileDescriptor.tell() + buffer.capacity());
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
        String parentPath = parent(path);
        FSNode parent = operatingSystem.getFileSystem().get(parentPath);
        if (parent == null) {
            this.error = "Directory not found: " + parentPath;
            this.errno = 2;
            return -1;
        }
        if (!(parent instanceof FSDirectory fsDirectory)) {
            this.error = "Not a directory: " + parentPath;
            this.errno = 2;
            return -1;
        }
        if (!(fsDirectory instanceof FSRoot)) fsDirectory.open();
        String fname = fname(path);
        fsDirectory.createDirectory(fname);
        if (!(fsDirectory instanceof FSRoot)) fsDirectory.close();

        if (!(fsDirectory instanceof FSRoot)) fsDirectory.open();
        FSNode child1 = fsDirectory.getChild(fname);
        if (child1 == null)
            throw new FileSystemIoException("Failed to write directory to disk!");
        if (!(fsDirectory instanceof FSRoot)) fsDirectory.close();
        return 0;
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

    public String strerror() {
        return error;
    }

    public int errno() {
        return errno;
    }

    public record FStat(long st_size, long st_mode, long st_atime, long st_mtime, long st_ctime) {

    }
}
