package dev.ultreon.devicesnext.filesystem;

import com.badlogic.gdx.utils.ObjectIntMap;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.*;

public class CraftFS implements FS, AutoCloseable {
    private final FileSystem fileSystem;
    private final ObjectIntMap<PathHandle> sharedLocks = new ObjectIntMap<>();
    private final Set<PathHandle> exclusiveLocks = new HashSet<>();

    public CraftFS(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }

    @Override
    public FSByteChannel open(PathHandle path, OpenOption... options) throws IOException {
        FSDirectory dir = getParentDirNode(path);
        FSNode node = dir.getChild(path.getName());
        Set<OpenOption> optionSet = new HashSet<>(Arrays.asList(options));
        if (node == null && (optionSet.contains(StandardOpenOption.CREATE) || optionSet.contains(StandardOpenOption.CREATE_NEW))) {
            lockPath(path, optionSet);
            dir.createFile(path.getName());
            node = dir.getChild(path.getName());
        } else if (node == null) {
            throw new FileNotFoundException(path.toString());
        } else if (optionSet.contains(StandardOpenOption.CREATE_NEW)) {
            throw new FileAlreadyExistsException("File already exists");
        }
        if (!node.isFile()) throw new FileNotFoundException(path.toString());
        FSFile file = (FSFile) node;
        file.open();
        lockPath(path, optionSet);
        return new FSByteChannel(path, file, optionSet);

    }

    private void lockPath(PathHandle path, Set<OpenOption> openOptions) throws IOException {
        if (openOptions.contains(StandardOpenOption.WRITE)) {
            if (this.sharedLocks.containsKey(path) || this.exclusiveLocks.contains(path))
                throw new IOException("File is locked by another process");
            this.exclusiveLocks.add(path);
        } else if (openOptions.contains(StandardOpenOption.READ)) {
            if (this.exclusiveLocks.contains(path))
                throw new IOException("File is locked by another process");
            this.sharedLocks.getAndIncrement(path, 0, 1);
        }
    }

    private void unlockPath(PathHandle path, Set<OpenOption> openOptions) throws IOException {
        if (openOptions.contains(StandardOpenOption.WRITE)) {
            this.exclusiveLocks.remove(path);
        } else if (openOptions.contains(StandardOpenOption.READ)) {
            int i = this.sharedLocks.get(path, 0);
            if (i <= 0) throw new IOException("File lock damaged");
            else if (i == 1) this.sharedLocks.remove(path, 0);
            else this.sharedLocks.put(path, i - 1);
        }
    }

    @Override
    public boolean exists(PathHandle path) throws IOException {
        FSNode node = fileSystem.root();
        if (node == null) throw new FileSystemException(path.toString(), null, "File system is not initialized");
        if (!node.isDirectory()) return false;

        for (String element : path.getParent()) {
            if (node == null || !node.isDirectory()) return false;

            FSDirectory dir = (FSDirectory) node;
            dir.setLastAccessed(System.currentTimeMillis());
            node.open();
            FSNode oldNode = node;
            node = node.getChild(element);
            oldNode.close();
        }

        return true;
    }

    @Override
    public void flush() throws IOException {
        fileSystem.flush();
    }

    @Override
    public void createFile(PathHandle path, byte[] data) throws IOException {
        FSDirectory dir = getParentDirNode(path);
        dir.createFile(path.getName());
        FSNode child = dir.getChild(path.getName());

        if (child == null) throw new FileSystemException(path.toString(), null, "Failed to create file");

        try (var channel = open(path, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(data));
        }
    }

    @Override
    public void createDirectory(PathHandle path) throws IOException {
        FSDirectory dir = getParentDirNode(path);
        dir.createDirectory(path.getName());

        FSNode child = dir.getChild(path.getName());

        if (child == null) throw new FileSystemException(path.toString(), null, "Failed to create directory");
        if (child instanceof FSFile)
            throw new FileSystemException(path.toString(), null, "Failed to create directory, file was created!?");

        FSDirectory createdDir = (FSDirectory) child;
        createdDir.setCreated(System.currentTimeMillis());
        createdDir.setLastModified(System.currentTimeMillis());
        createdDir.setLastAccessed(System.currentTimeMillis());
    }

    @Override
    public Iterator<String> listDirectory(PathHandle path) throws IOException {
        FSDirectory directory = getParentDirNode(path);

        directory.open();
        FSNode[] list = directory.list();
        directory.close();
        return new DirNameIterator(list);
    }

    private @NotNull FSDirectory getParentDirNode(PathHandle path) throws FileSystemException, FileNotFoundException {
        FSNode node = fileSystem.root();
        if (node == null) throw new FileSystemException(path.toString(), null, "File system is not initialized");
        if (!node.isDirectory()) throw new FileNotFoundException(path.toString());

        for (String element : path.getParent()) {
            if (!node.isDirectory()) throw new NotDirectoryException(path.getParent().toString());

            FSDirectory dir = (FSDirectory) node;
            dir.setLastAccessed(System.currentTimeMillis());
            node.open();
            FSNode oldNode = node;
            node = node.getChild(element);
            oldNode.close();


            if (node == null)
                throw new FileNotFoundException(path.toString());
        }

        if (!node.isDirectory()) throw new NotDirectoryException(path.getParent().toString());
        FSDirectory dir = (FSDirectory) node;
        dir.open();
        dir.setLastAccessed(System.currentTimeMillis());
        return dir;
    }

    @Override
    public void delete(PathHandle path) throws IOException {
        FSDirectory directory = getParentDirNode(path);
        directory.getChild(path.getName());
        directory.delete(path.getName());
        directory.close();
    }

    @Override
    public long size(PathHandle path) throws IOException {
        return 0;
    }

    @Override
    public void rename(PathHandle from, String name) throws IOException {
        FSDirectory fromDir = getParentDirNode(from);
        FSNode child = fromDir.getChild(name);
        fromDir.rename(name, child, true);
    }

    @Override
    public void atomicMove(PathHandle from, PathHandle to) throws IOException {
        FSDirectory fromParent = getParentDirNode(from);
        FSDirectory toParent = getParentDirNode(to);

        FSNode child = fromParent.getChild(from.getName());
        fromParent.move(from.getName(), toParent, true);
        toParent.rename(to.getName(), child, true);
    }

    @Override
    public FileInfo info(PathHandle path) throws IOException {
        FSNode node = fileSystem.root();
        if (node == null) throw new FileSystemException(path.toString(), null, "File system is not initialized");
        if (!node.isDirectory()) return new FileInfo(0, false, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, path.getName());

        for (String element : path.getParent()) {
            if (!node.isDirectory()) return new FileInfo(0, false, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, path.getName());

            FSDirectory dir = (FSDirectory) node;
            dir.setLastAccessed(System.currentTimeMillis());
            node.open();
            FSNode oldNode = node;
            node = node.getChild(element);
            oldNode.close();

            if (node == null) return new FileInfo(0, false, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, path.getName());
        }

        if (!node.isDirectory()) throw new NotDirectoryException(path.getParent().toString());
        FSDirectory directory = (FSDirectory) node;
        directory.setLastAccessed(System.currentTimeMillis());
        directory.createDirectory(path.getName());
        directory.getChild(path.getName());

        FSNode fsNode = directory.getChild(path.getName());
        directory.close();
        return switch (fsNode) {
            case null -> throw new FileNotFoundException(path.toString());
            case FSFile file -> new FileInfo(file.getLength(), false, true, file.getMode(), file.getLastModified(), file.getCreated(), file.getLastAccessed(), 20, 500, hashCode(), path.hashCode(), 1, hashCode(), 1024, file.getAddress(), file.getName());
            case FSDirectory dir -> new FileInfo(0, true, false, 0b111101101, dir.getLastModified(), dir.getCreated(), dir.getLastAccessed(), 20, 500, hashCode(), path.hashCode(), 1, hashCode(), 1024, dir.getAddress(), dir.getName());
            default -> throw new FileSystemException(path.toString(), null, "Neither a file or directory");
        };
    }

    @Override
    public void copy(PathHandle from, PathHandle toDir, boolean overwrite) throws IOException {
        FSDirectory fromDir = getParentDirNode(from);
        FSNode child = fromDir.getChild(from.getName());

        if (child == null || !child.isFile()) throw new FileNotFoundException(from.toString());

        FSDirectory toParent = getParentDirNode(toDir);
        FSNode toDirNode = toParent.getChild(toDir.getName());
        if (!toDirNode.isDirectory()) throw new NotDirectoryException(toDir.toString());

        IOUtils.copy(new FSInputStream(open(from, StandardOpenOption.READ)), new FSOutputStream(open(toDir.child(from.getName()), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)));
    }

    public class FSByteChannel implements SeekableByteChannel {
        private final PathHandle path;
        private final FSFile file;
        private final Set<OpenOption> options;
        private long offset;
        private long position;

        public FSByteChannel(PathHandle path, FSFile file, Set<OpenOption> options) throws IOException {
            this.path = path;
            this.file = file;
            this.options = options;
            offset = 0L;

            if (options.contains(StandardOpenOption.APPEND) && options.contains(StandardOpenOption.READ)) throw new IOException("Can't open file for reading and appending");

            if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) file.setLength(0);
            if (options.contains(StandardOpenOption.APPEND)) offset = file.getLength();
        }

        public FSFile getFile() {
            return file;
        }

        public boolean isReadable() {
            return options.contains(StandardOpenOption.READ);
        }

        public boolean isWritable() {
            return options.contains(StandardOpenOption.WRITE);
        }

        @Override
        public boolean isOpen() {
            return options.contains(StandardOpenOption.WRITE) ? exclusiveLocks.contains(path) : sharedLocks.containsKey(path);
        }

        @Override
        public void close() throws IOException {
            checkLock();
            unlockPath(path, options);
            file.flush();
            file.close();
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            if (!options.contains(StandardOpenOption.READ)) throw new IOException("File is not open for reading");
            checkLock();
            file.read(offset, dst);
            return dst.remaining();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            if (options.contains(StandardOpenOption.APPEND)) {
                checkLock();
                offset = file.getLength();
                int pos = src.position();
                file.write(offset, src);
                return src.position() - pos;
            }
            if (!options.contains(StandardOpenOption.WRITE)) throw new IOException("File is not open for writing");
            checkLock();
            int pos = src.position();
            file.setLength(Math.max(file.length, offset + src.remaining()));
            file.write(offset, src);
            file.flush();
            file.fs.flush();
            return src.position() - pos;
        }

        @Override
        public long position() throws IOException {
            checkLock();
            return position;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            checkLock();
            this.position = newPosition;
            return this;
        }

        private void checkLock() throws FileSystemException {
            if (options.contains(StandardOpenOption.WRITE) && !exclusiveLocks.contains(path))
                throw new FileSystemException(path.toString(), null, "Lost file lock");
            if (options.contains(StandardOpenOption.READ) && !sharedLocks.containsKey(path))
                throw new FileSystemException(path.toString(), null, "Lost file lock");
        }

        @Override
        public long size() throws IOException {
            checkLock();
            return file.getLength();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            if (!options.contains(StandardOpenOption.WRITE)) throw new IOException("File is not open for writing");
            checkLock();
            file.setLength(size);
            return this;
        }
    }
}
