package dev.ultreon.devicesnext.virtual;

import dev.ultreon.devicesnext.filesystem.FileInfo;
import dev.ultreon.devicesnext.filesystem.PathHandle;
import dev.ultreon.devicesnext.mineos.VirtualComputer;
import org.graalvm.polyglot.io.FileSystem;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipal;
import java.util.*;

import static java.lang.System.currentTimeMillis;

@SuppressWarnings("t")
public class VirtualFileSystem implements FileSystem {
    private final FileSystem delegate = FileSystem.newDefaultFileSystem();
    private Path currentWorkingDirectory = Path.of("/");
    private VirtualComputer virtualComputer;

    public VirtualFileSystem(VirtualComputer virtualComputer) {
        this.virtualComputer = virtualComputer;
    }

    @Override
    public Path parsePath(URI uri) {
        return delegate.parsePath(uri);
    }

    @Override
    public Path parsePath(String path) {
        return delegate.parsePath(path);
    }

    @Override
    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
        if (isInternal(path)) {
            delegate.checkAccess(path, modes, linkOptions);
            return;
        }
        PathHandle virtualPath = getVirtualPath(path);
        if (!virtualComputer.getFileSystem().exists(virtualPath)) {
            throw new FileNotFoundException(virtualPath.toString());
        }
    }

    private PathHandle getVirtualPath(Path path) {
        String string = path.toString();
        if (System.getProperty("os.name").equals("Windows")) {
            if (string.startsWith("/") || string.startsWith("\\")) {
                return new PathHandle(string.replace("\\", "/"));
            } else if (string.matches("^[a-zA-Z]:.*$")) {
                return new PathHandle(string.substring(2).replace("\\", "/"));
            } else {
                return new PathHandle(string.replace("\\", "/"));
            }
        } else {
            return new PathHandle(string);
        }
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... attrs) throws IOException {
        if (isInternal(path)) {
            throw new IOException("Internal path is read-only!");
        }
        
        path = path.isAbsolute() ? path : currentWorkingDirectory.resolve(path);

        if (path.equals(Path.of("/"))) {
            throw new FileAlreadyExistsException("Cannot create root directory");
        }
        virtualComputer.getFileSystem().createDirectory(getVirtualPath(path));
    }

    @Override
    public void delete(Path path) throws IOException {
        if (isInternal(path)) {
            throw new IOException("Internal path is read-only!");
        }
        path = path.isAbsolute() ? path : currentWorkingDirectory.resolve(path);

        virtualComputer.getFileSystem().delete(getVirtualPath(path));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        if (isInternal(path)) {
            if (options.contains(StandardOpenOption.WRITE)) {
                throw new IOException("Internal path is read-only!");
            }

            return delegate.newByteChannel(path, options, attrs);
        }

        path = path.isAbsolute() ? path : currentWorkingDirectory.resolve(path);
        return virtualComputer.getFileSystem().open(getVirtualPath(path), options.toArray(OpenOption[]::new));
    }

    private static boolean isInternal(Path path) {
        if (path.startsWith(Path.of(switch (System.getProperty("os.name")) {
            case "Linux" -> "/home/" + System.getProperty("user.name") + "/.cache/org.graalvm.polyglot";
            case "Mac OS X" -> "/Users/" + System.getProperty("user.name") + "/Library/Caches/org.graalvm.polyglot";
            case "Windows" ->
                    "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\org.graalvm.polyglot";
            default -> throw new IllegalStateException();
        }))) {
            return true;
        }
        return path.toString().matches("<.*>");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter) throws IOException {
        if (isInternal(path)) {
            return delegate.newDirectoryStream(path, filter);
        }
        path = path.isAbsolute() ? path : currentWorkingDirectory.resolve(path);
        Iterator<String> stringIterator = virtualComputer.getFileSystem().listDirectory(getVirtualPath(path));

        Path finalPath = path;
        return new DirectoryStream<Path>() {
            @Override
            public void close() throws IOException {
                // No need
            }

            @Override
            public Iterator<Path> iterator() {
                return new Iterator<Path>() {
                    @Override
                    public boolean hasNext() {
                        return stringIterator.hasNext();
                    }

                    @Override
                    public Path next() {
                        return finalPath.resolve(stringIterator.next());
                    }
                };
            }
        };
    }

    @Override
    public Path toAbsolutePath(Path path) {
        return path.toAbsolutePath();
    }

    @Override
    public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
        return path.toRealPath(linkOptions);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        if (isInternal(path)) {
            Map<String, Object> stringObjectMap = delegate.readAttributes(path, attributes, options);
            if (isInternal(path) && stringObjectMap.containsKey("mode") && stringObjectMap.get("mode") instanceof Integer) {
                int mode = (int) stringObjectMap.get("mode");

                // Remove write flags (0x2, 0x4, 0x8) from the unix file mode.
                mode &= ~0x2;
                mode &= ~0x4;
                mode &= ~0x8;
                stringObjectMap.put("mode", mode);
            }

            return stringObjectMap;
        }

        path = path.isAbsolute() ? path : currentWorkingDirectory.resolve(path);
        FileInfo fileInfo = virtualComputer.getFileSystem().info(getVirtualPath(path));
        if (fileInfo == null) {
            throw new IOException("Failed to get file info");
        }

        Map<String, Object> map = new HashMap<>();
        if (attributes.startsWith("unix:")) {
            attributes = attributes.substring("unix:".length());
        } else if (attributes.startsWith("basic:")) {
            attributes = attributes.substring("basic:".length());
        } else if (attributes.contains(":")) {
            throw new IllegalArgumentException("Unsupported attributes: " + attributes);
        }

        for (@NotNull String entry : attributes.split(",")) {
            switch (entry) {
                case "size" -> map.put("size", fileInfo.size());
                case "isDirectory" -> map.put("isDirectory", fileInfo.isDirectory());
                case "isRegularFile" -> map.put("isRegularFile", fileInfo.isFile());
                case "isSymbolicLink" -> map.put("isSymbolicLink", false);
                case "uid" -> map.put("uid", 20);
                case "gid" -> map.put("gid", 500);
                case "owner" -> map.put("owner", (UserPrincipal) () -> "user");
                case "permissions" -> map.put("permissions", fileInfo.mode());
                case "creationTime" -> map.put("creationTime", FileTime.fromMillis(fileInfo.ctime()));
                case "lastAccessed" -> map.put("lastAccessed", FileTime.fromMillis(fileInfo.atime()));
                case "lastAccessedTime" -> map.put("lastAccessedTime", FileTime.fromMillis(fileInfo.atime()));
                case "lastAccessTime" -> map.put("lastAccessTime", FileTime.fromMillis(fileInfo.atime()));
                case "lastModified" -> map.put("lastModified", FileTime.fromMillis(fileInfo.mode()));
                case "lastModifiedTime" -> map.put("lastModifiedTime", FileTime.fromMillis(fileInfo.mode()));
                case "createdTime" -> map.put("createdTime", FileTime.fromMillis(fileInfo.ctime()));
                case "inode" -> map.put("inode", fileInfo.ino());
                case "fileKey" -> map.put("fileKey", 0);
                case "ino" -> map.put("ino", fileInfo.ino());
                case "rdev" -> map.put("rdev", fileInfo.dev());
                case "atime" -> map.put("atime", FileTime.fromMillis(currentTimeMillis()));
                case "mtime" -> map.put("mtime", FileTime.fromMillis(currentTimeMillis()));
                case "ctime" -> map.put("ctime", FileTime.fromMillis(currentTimeMillis()));
                case "dev" -> map.put("dev", fileInfo.dev());
                case "nlink" -> map.put("nlink", fileInfo.nlink());
                case "mode" -> map.put("mode", fileInfo.mode());
                default -> throw new IOException("Unknown attribute: " + entry);
            }
        }
        return map;
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        if (isInternal(source) || isInternal(target)) {
            throw new IOException("Cannot copy internal files");
        }

        boolean overwrite = isOverwrite(options);

        FileInfo fileInfo = virtualComputer.getFileSystem().info(getVirtualPath(source));
        source = source.isAbsolute() ? source : currentWorkingDirectory.resolve(source);
        target = target.isAbsolute() ? target : currentWorkingDirectory.resolve(target);

        if (fileInfo == null) {
            throw new IOException("Failed to get file info");
        }
        virtualComputer.getFileSystem().copy(getVirtualPath(source), getVirtualPath(target), overwrite);
    }

    private static boolean isOverwrite(CopyOption[] options) throws IOException {
        boolean overwrite = false;
        for (CopyOption option : options) {
            switch (option) {
                case StandardCopyOption.REPLACE_EXISTING -> overwrite = true;
                case StandardCopyOption.COPY_ATTRIBUTES -> {
                    // Ignore
                }
                case StandardCopyOption.ATOMIC_MOVE -> throw new IOException("Atomic move not supported");
                case null, default -> throw new IOException("Option not supported: " + option);
            }
        }
        return overwrite;
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        if (isInternal(source) || isInternal(target)) {
            throw new IOException("Cannot move internal files");
        }

        boolean overwrite = isOverwrite(options);

        source = source.isAbsolute() ? source : currentWorkingDirectory.resolve(source);
        target = target.isAbsolute() ? target : currentWorkingDirectory.resolve(target);
        virtualComputer.getFileSystem().atomicMove(getVirtualPath(source), getVirtualPath(target));
    }

    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        throw new IOException("Symbolic links not supported");
    }

    @Override
    public void createLink(Path link, Path existing) throws IOException {
        throw new IOException("Hard links not supported");
    }

    @Override
    public void setCurrentWorkingDirectory(Path currentWorkingDirectory) {
        this.delegate.setCurrentWorkingDirectory(currentWorkingDirectory);

        this.currentWorkingDirectory = currentWorkingDirectory;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public String getPathSeparator() {
        return ":";
    }

    @Override
    public Path getTempDirectory() {
        return Path.of("/tmp");
    }

    @Override
    public Charset getEncoding(Path path) {
        return StandardCharsets.UTF_8;
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        throw new IOException("Symbolic links not supported");
    }
}
