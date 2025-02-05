package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSDirectory;
import dev.ultreon.devicesnext.device.hardware.FSFile;
import dev.ultreon.devicesnext.device.hardware.FSNode;
import dev.ultreon.devicesnext.device.hardware.FSRoot;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

@SuppressWarnings("SpellCheckingInspection")
public class LibStd implements SystemLibrary {
    private final OperatingSystemImpl operatingSystem;
    private final FileDescriptorManager fdManager;
    public static final int O_CREAT = 0x4000;
    public static final int O_RDONLY = 0x0002;
    public static final int O_WRONLY = 0x0001;
    public static final int O_TRUNC = 0x2000;

    public static final int EPERM = 1;
    public static final int ENOENT = 2;
    public static final int ENXIO = 5;
    public static final int E2BIG = 7;
    public static final int EBADF = 9;
    public static final int ENOMEM = 12;
    public static final int EACCES = 13;
    public static final int ENOTBLK = 15;
    public static final int EBUSY = 16;
    public static final int EEXIST = 17;
    public static final int ENODEV = 19;
    public static final int ENOTDIR = 20;
    public static final int EINVAL = 22;
    public static final int EISDIR = 21;
    public static final int ENOSPC = 28;
    public static final int ESPIPE = 29;
    public static final int EROFS = 30;
    public static final int EPIPE = 32;
    public static final int ENOSYS = 38;

    public static final int SEEK_SET = 0;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;

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
                this.errno = ENOENT;
                return -1;
            }

            fsNode.open();
            if (!(fsNode instanceof FSDirectory fsDirectory)) {
                this.error = "Not a directory: " + parent;
                this.errno = ENOTDIR;
                return -1;
            }

            fsDirectory.open();
            fsDirectory.createFile(fname(path));
            fsDirectory.close();
        }

        FSNode fsNode = operatingSystem.getFileSystem().get(path);
        if (fsNode == null) {
            this.error = "File not found: " + path;
            this.errno = ENOENT;
            return -1;
        }
        if (!(fsNode instanceof FSFile fsFile)) {
            this.error = "Not a file: " + path;
            this.errno = EISDIR;
            return -1;
        }
        return operatingSystem.gerFdManager().open(path, fsFile, flags);
    }

    boolean isdir(String path) {
        FSNode fsNode = operatingSystem.getFileSystem().get(path);
        if (fsNode == null) {
            this.error = "File not found: " + path;
            this.errno = ENOENT;
            return false;
        }
        return fsNode instanceof FSDirectory;
    }

    boolean isfile(String path) {
        FSNode fsNode = operatingSystem.getFileSystem().get(path);
        if (fsNode == null) {
            this.error = "File not found: " + path;
            this.errno = ENOENT;
            return false;
        }
        return fsNode instanceof FSFile;
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
            errno = EINVAL;
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
            errno = EINVAL;
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
            errno = EINVAL;
            return -1;
        }

        buffer.flip();
        buffer.position(0);
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        fileDescriptor.truncate(fileDescriptor.tell() + buffer.capacity());
        fileDescriptor.write(buffer);
        return buffer.position();
    }

    int lseek(int fd, long offset, int whence) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = EINVAL;
            return -1;
        }
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        switch (whence) {
            case SEEK_SET -> fileDescriptor.seek(offset);
            case SEEK_CUR -> fileDescriptor.seek(fileDescriptor.tell() + offset);
            case SEEK_END -> fileDescriptor.seek(fileDescriptor.getFile().getLength() + offset);
        }
        return (int) fileDescriptor.tell();
    }

    FStat stat(String path) {
        FSNode fsNode = operatingSystem.getFileSystem().get(path);
        if (fsNode == null) {
            this.error = "File not found: " + path;
            this.errno = ENOENT;
            return null;
        }
        if (!(fsNode instanceof FSFile fsFile)) {
            this.error = "Not a file: " + path;
            this.errno = EISDIR;
            return null;
        }
        return new FStat(fsFile.getLength(), fsFile.getMode(), fsFile.getLastAccessed(), fsFile.getLastModified(), fsFile.getCreated());
    }

    FStat fstat(int fd) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = EINVAL;
            return null;
        }
        FSFile file = operatingSystem.gerFdManager().get(fd).getFile();
        return new FStat(file.getLength(), file.getMode(), file.getLastAccessed(), file.getLastModified(), file.getCreated());
    }

    int ftruncate(int fd, long size) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = EINVAL;
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
        if (path.equals("/")) return 0;

        if (isdir(path)) {
            this.error = "File already exists: " + path;
            this.errno = EEXIST;
            return -1;
        }

        String parentPath = parent(path);
        FSNode parent = operatingSystem.getFileSystem().get(parentPath);
        if (parent == null) {
            this.error = "Directory not found: " + parentPath;
            this.errno = ENOENT;
            return -1;
        }
        if (!(parent instanceof FSDirectory fsDirectory)) {
            this.error = "Not a directory: " + parentPath;
            this.errno = ENOENT;
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

    int mkdirs(String path, int mode) {
        if (!path.startsWith("/")) {
            this.error = "Path must be absolute: " + path;
            this.errno = EINVAL;
            return -1;
        }
        if (path.equals("/")) return 0;

        @NotNull String[] split = path.split("/");
        for (int i = 1, splitLength = split.length; i < splitLength; i++) {
            String part = split[i];
            mkdir(part, mode);
        }
        return 0;
    }

    int rmdir(String path) {
        String parentPath = parent(path);
        FSNode parent = operatingSystem.getFileSystem().get(parentPath);
        if (parent == null) {
            this.error = "Directory not found: " + parentPath;
            this.errno = ENOENT;
            return -1;
        }
        if (!(parent instanceof FSDirectory fsDirectory)) {
            this.error = "Not a directory: " + parentPath;
            this.errno = ENODEV;
            return -1;
        }
        fsDirectory.open();
        fsDirectory.delete(fname(path));
        fsDirectory.close();
        return 0;
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

    public FStatVFS statvfs(String path) {
        // TODO
        return null;
    }

    public FStatVFS fstatvfs(int fd) {
        // TODO
        return null;
    }

    public int getpagesize() {
        // TODO
        return 4096;
    }

    public int isatty(int fd) {
        return 0; // Is never a TTY. This isn't Linux.
    }

    public int getuid() {
        // TODO
        return 0;
    }

    public int geteuid() {
        // TODO
        return 0;
    }

    public int getgid() {
        // TODO
        return 0;
    }

    public int getegid() {
        // TODO
        return 0;
    }

    public int getgroups() {
        // TODO
        return 0;
    }

    public String getenv(String name) {
        // TODO
        return null;
    }

    public void setenv(String name, String value) {
        // TODO
    }

    public int execv(String path, String[] args) {
        // TODO
        return -1;
    }

    public int execvp(String path, String[] args) {
        // TODO
        return -1;
    }

    public int execve(String path, String[] args, String[] envp) {
        // TODO
        return -1;
    }

    public int execvpe(String path, String[] args, String[] envp) {
        // TODO
        return -1;
    }

    public int fork() {
        // TODO
        return -1;
    }

    public int waitpid(int pid, int[] status) {
        // TODO
        return -1;
    }

    public int wait(int[] status) {
        // TODO
        return -1;
    }

    public int wait3(int[] status, int options) {
        // TODO
        return -1;
    }

    public int wait4(int pid, int[] status, int options) {
        // TODO
        return -1;
    }

    public void exit(int status) {
        // TODO
    }

    public void abort() {
        // TODO
    }

    public int getrlimit(int resource) {
        // TODO
        return -1;
    }

    public void puts(String s) {
        // TODO
    }

    public void perror(String s) {
        // TODO
    }

    public void clearerr() {
        error = null;
        errno = 0;
    }

    public void rewind(int fd) {
        // TODO
    }

    public void rewinddir(int fd) {
        // TODO
    }

    public void seek(int fd, long offset) {
        // TODO
    }

    public boolean feof(int fd) {
        FStat fstat = fstat(fd);
        if (fstat == null) return true;
        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        return fstat.st_size == fileDescriptor.tell();
    }

    public int getc(int fd) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = EINVAL;
            return -1;
        }

        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        if (fileDescriptor == null) {
            return -1;
        }
        if (!fileDescriptor.getFile().isOpen()) {
            fileDescriptor.getFile().close();
            error = "File not open";
            errno = EBADF;
            operatingSystem.gerFdManager().free(fileDescriptor);
            return -1;
        }
        ByteBuffer allocate = ByteBuffer.allocate(1);
        fileDescriptor.read(allocate);
        return allocate.get(0);
    }

    public int putc(int c, int fd) {
        if (fd < 0) {
            error = "Invalid file descriptor";
            errno = EINVAL;
            return -1;
        }

        FileDescriptor fileDescriptor = operatingSystem.gerFdManager().get(fd);
        if (fileDescriptor == null) {
            return -1;
        }
        if (!fileDescriptor.getFile().isOpen()) {
            fileDescriptor.getFile().close();
            error = "File not open";
            errno = EBADF;
            operatingSystem.gerFdManager().free(fileDescriptor);
            return -1;
        }
        ByteBuffer allocate = ByteBuffer.allocate(1);
        allocate.put((byte) c);
        fileDescriptor.write(allocate);
        return -1;
    }

    public void kill(int pid, int sig) {
        // TODO
    }

    public void alarm(int seconds) {
        // TODO
    }

    public void sleep(int seconds) {
        // TODO
    }

    public record FStat(long st_size, long st_mode, long st_atime, long st_mtime, long st_ctime) {

    }

    public record FStatVFS(long f_bsize, long f_frsize, long f_blocks, long f_bfree, long f_bavail, long f_files, long f_ffree) {

    }
}
