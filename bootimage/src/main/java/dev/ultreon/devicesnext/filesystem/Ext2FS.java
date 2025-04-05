package dev.ultreon.devicesnext.filesystem;

import com.badlogic.gdx.utils.ObjectIntMap;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.driver.virtual.VirtualDevice;
import org.jnode.fs.*;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.ext2.*;
import org.jnode.fs.spi.AbstractFileSystem;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.*;

@SuppressWarnings("t")
public class Ext2FS implements FS {
    private final FileSystem<?> fs;

    private final ObjectIntMap<PathHandle> sharedLocks = new ObjectIntMap<>();
    private final Set<PathHandle> exclusiveLocks = new HashSet<>();

    private Ext2FS(Ext2FileSystem fs) {
        this.fs = fs;

        Cleaner cleaner = Cleaner.create();
        cleaner.register(this, () -> {
            try {
                fs.close();
            } catch (IOException e) {
                LoggerFactory.getLogger(Ext2FS.class).error("Failed to close filesystem", e);
            }
        });
    }

    public static Ext2FS open(Path filePath) throws IOException, FileSystemException {
        return open(false, filePath);
    }

    public static Ext2FS open(boolean readOnly, Path filePath) throws IOException, FileSystemException {
        var device = new VirtualDevice("MineDisk");
        var blockDevice = new VirtualBlockDevice(filePath.toFile().getAbsolutePath(), Files.size(filePath));
        device.registerAPI(BlockDeviceAPI.class, blockDevice);

        var type = new Ext2FileSystemType();
        var fs = new Ext2FileSystem(device, readOnly, type);
        fs.read();
        return new Ext2FS(fs);
    }

    public static Ext2FS openForced(Path filePath) throws IOException, FileSystemException {
        var device = new VirtualDevice("MineDisk");
        var blockDevice = new VirtualBlockDevice(filePath.toFile().getAbsolutePath(), Files.size(filePath));
        device.registerAPI(BlockDeviceAPI.class, blockDevice);

        var type = new Ext2FileSystemType();
        var fs = new Ext2FileSystem(device, false, type);
        fs.getSuperblock().setState(Ext2Constants.EXT2_VALID_FS);
        fs.flush();
        fs.read();
        return new Ext2FS(fs);
    }

    public static Ext2FS format(Path filePath, long diskSize) throws IOException, FileSystemException {
        if (diskSize <= 16384) throw new IllegalArgumentException("Disk size must be greater than 16 KiB");

        var device = new VirtualDevice("MineDisk");
        device.registerAPI(BlockDeviceAPI.class, new VirtualBlockDevice(filePath.toFile().getAbsolutePath(), diskSize));

        var blockDevice = new VirtualBlockDevice(filePath.toFile().getAbsolutePath(), diskSize);
        var formatter = new Ext2FileSystemFormatter(BlockSize._1Kb);
        var fs = formatter.format(device);
        return new Ext2FS(fs);
    }

    @Override
    public void close() throws IOException {
        fs.close();
    }

    @Override
    public SeekableByteChannel open(PathHandle path, OpenOption... options) throws IOException {
        Ext2Directory dir = getDirectoryAt(path.getParent());
        if (dir == null) {
            throw new FileNotFoundException(path.getParent().toString());
        }
        FSEntry node = dir.getEntry(path.getName());
        Set<OpenOption> optionSet = new HashSet<>(Arrays.asList(options));
        if (node == null && (optionSet.contains(StandardOpenOption.CREATE) || optionSet.contains(StandardOpenOption.CREATE_NEW))) {
            dir.addFile(path.getName());
            node = dir.getEntry(path.getName());
        } else if (node == null) {
            throw new FileNotFoundException(path.toString());
        } else if (optionSet.contains(StandardOpenOption.CREATE_NEW)) {
            throw new FileAlreadyExistsException("File already exists");
        }
        if (!node.isFile()) throw new FileNotFoundException(path.toString());
        lockPath(path, optionSet);
        return new FSByteChannel(path, node, node.getFile(), options);
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
            if (!this.exclusiveLocks.contains(path)) throw new IOException("File lock damaged");
            this.exclusiveLocks.remove(path);
        } else if (openOptions.contains(StandardOpenOption.READ)) {
            int i = this.sharedLocks.get(path, 0);
            if (i <= 0) throw new IOException("File lock damaged");
            else if (i == 1) this.sharedLocks.remove(path, 0);
            else this.sharedLocks.put(path, i - 1);
        }
    }

    public InputStream read(PathHandle path, OpenOption... options) throws IOException {
        FSFile file = getFileAt(path);
        if (file == null) throw new NoSuchFileException("File not found: " + path);
        return new FSInputStream(file, options);
    }

    public OutputStream write(PathHandle path, OpenOption... options) throws IOException {
        boolean create = false;
        boolean createNew = false;
        for (OpenOption option : options) {
            if (option == StandardOpenOption.CREATE) {
                create = true;
            } else if (option == StandardOpenOption.CREATE_NEW) {
                createNew = true;
            }
        }

        if (create && createNew) throw new IllegalArgumentException("Cannot create and createNew");

        FSFile file;
        if (create) {
            Ext2Directory parentDir = getDirectoryAt(path.getParent());
            if (parentDir == null) {
                throw new NoSuchFileException(path.getParent().toString(), null, "parent directory not found");
            }
            Ext2Entry entry = (Ext2Entry) parentDir.getEntry(path.getName());
            if (entry == null) {
                file = parentDir.addFile(path.getName()).getFile();
            } else if (entry.isFile()) {
                file = entry.getFile();
            } else {
                throw new FileAlreadyExistsException("Directory already exists: " + path);
            }
        } else if (createNew) {
            Ext2Directory parentDir = getDirectoryAt(path.getParent());
            if (parentDir == null) {
                throw new NoSuchFileException(path.getParent().toString(), null, "parent directory not found");
            }
            if (parentDir.getEntry(path.getName()) != null) {
                throw new FileAlreadyExistsException("File already exists: " + path);
            }
            file = parentDir.addFile(path.getName()).getFile();
        } else {
            file = getFileAt(path);
            if (file == null) throw new NoSuchFileException(path.toString(), null, "file not found");
        }
        return new FSOutputStream(file, options);
    }

    @Override
    public boolean exists(PathHandle path) {
        try {
            return getFsEntry(path) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private FSFile getFileAt(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return null;
        return entry.getFile();
    }

    private Ext2Directory getDirectoryAt(PathHandle path) throws IOException {
        Preconditions.checkNotNull(path, "path");
        Ext2Entry fsEntry = getFsEntry(path);
        if (fsEntry == null) return null;
        if (!fsEntry.isDirectory()) throw new IllegalArgumentException("PathHandle is not a directory: " + path);
        return (Ext2Directory) fsEntry.getDirectory();
    }

    @Nullable
    public Ext2Entry getFsEntry(PathHandle path) throws IOException {
        if (path.getParent() == null) return (Ext2Entry) fs.getRootEntry();

        Ext2Directory root = (Ext2Directory) fs.getRootEntry().getDirectory();
        for (String s : path.getParent()) {
            Ext2Entry entry = (Ext2Entry) root.getEntry(s);
            if (entry == null) return null;
            if (!entry.isDirectory()) return null;
            root = (Ext2Directory) entry.getDirectory();
            if (root == null) return null;
        }
        return (Ext2Entry) root.getEntry(path.getName());
    }

    @Override
    public void flush() throws IOException {
        if (fs instanceof AbstractFileSystem<?>) ((AbstractFileSystem) fs).flush();
    }

    @Override
    public void createFile(PathHandle path, byte[] data) throws IOException {
        if (!path.toString().startsWith("/")) path = new PathHandle("/" + path);
        if (path.toString().equals("/")) throw new IOException("Invalid path for file: " + path);

        Ext2Directory parentDir = path.getParent() == null ? (Ext2Directory) fs.getRootEntry().getDirectory() : getDirectoryAt(path.getParent());
        if (parentDir == null)
            throw new NoSuchFileException(path.getParent().toString(), null, "parent directory not found");
        parentDir.addFile(path.getName());
        Ext2Entry entry = (Ext2Entry) parentDir.getEntry(path.getName());
        FSFile file = entry.getFile();
        file.flush();
        parentDir.flush();
        if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
        file.setLength(0);
        file.setLength(data.length);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        file.write(0L, buffer);
        parentDir.flush();
    }

    @Override
    public void createDirectory(PathHandle path) throws IOException {
        if (!path.toString().startsWith("/")) path = new PathHandle("/" + path);
        Ext2Directory parentDir = path.getParent() == null ? (Ext2Directory) fs.getRootEntry().getDirectory() : getDirectoryAt(path.getParent());
        if (path.getParent() != null && parentDir != null) {
            if (parentDir.getEntry(path.getName()) != null) {
                throw new FileAlreadyExistsException(path.toString());
            }
            parentDir.addDirectory(path.getName());
            parentDir.flush();
        } else {
            if (fs.getRootEntry().getDirectory().getEntry(path.getName()) != null) {
                throw new FileAlreadyExistsException(path.toString());
            }
            FSDirectory directory = fs.getRootEntry().getDirectory();
            directory.addDirectory(path.getName());
            directory.flush();
        }
        if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
    }

    @Override
    public Iterator<String> listDirectory(PathHandle of) throws IOException {
        Ext2Directory dir = getDirectoryAt(of);
        if (dir == null) return Collections.emptyIterator();
        return new DirNameIterator(dir);
    }

    @Override
    public void delete(PathHandle path) throws IOException {
        Ext2Entry parent = path.getParent() == null ? (Ext2Entry) fs.getRootEntry() : getFsEntry(path.getParent());
        if (parent == null) return;

        if (!parent.isDirectory()) throw new NotDirectoryException("PathHandle is not a directory: " + path);

        Ext2Entry entry = (Ext2Entry) parent.getDirectory().getEntry(path.getName());
        if (entry.isDirectory()) {
            if (entry.getDirectory().iterator().hasNext()) {
                throw new DirectoryNotEmptyException("Directory is not empty: " + path);
            }

            parent.getDirectory().remove(path.getName());
            return;
        }

        if (!entry.isFile()) throw new IOException("PathHandle is not a file: " + path);
        parent.getDirectory().remove(path.getName());
    }

    @Override
    public long size(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return -1;
        if (!entry.isFile()) return -1;
        return entry.getFile().getLength();
    }

    @Override
    public void rename(PathHandle from, String name) throws IOException {
        if (name.contains("/")) throw new IOException("Invalid name: " + name);

        Ext2Entry parent = from.getParent() == null ? (Ext2Entry) fs.getRootEntry() : getFsEntry(from.getParent());
        if (parent == null) return;

        if (!parent.isDirectory()) throw new NotDirectoryException("PathHandle is not a directory: " + from);

        Ext2Entry entry = (Ext2Entry) parent.getDirectory().getEntry(from.getName());
        if (entry == null) return;
        entry.setName(name);
        parent.getDirectory().flush();
    }

    @Override
    public void atomicMove(PathHandle from, PathHandle to) throws IOException {
        FSEntry fromEntry = getFsEntry(from);
        FSEntry toEntry = getFsEntry(to);

        if (fromEntry == null || toEntry == null) return;
        if (from.getParent().equals(to)) {
            fromEntry.setName(to.getName());
            toEntry.getParent().flush();
        } else {
            throw new java.nio.file.FileSystemException("Cannot move files between different directories [FS_NOT_IMPLEMENTED]");
        }
    }

    @Override
    public FileInfo info(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) throw new FileNotFoundException(path.toString());
        return new FileInfo(
                entry.isFile() ? entry.getFile().getLength() : 0,
                entry.isDirectory(),
                entry.isFile(),
                entry.getINode().getMode(),
                entry.getINode().getMtime(),
                entry.getINode().getCtime(),
                entry.getINode().getAtime(),
                entry.getINode().getUid(),
                entry.getINode().getGid(),
                hashCode(),
                path.hashCode(),
                1,
                hashCode(),
                ((Ext2FileSystem)fs).getBlockSize(),
                entry.getINode().getBlocks(),
                entry.getName()
        );
    }

    @Override
    public void copy(PathHandle virtualPath, PathHandle virtualPath1, boolean overwrite) throws IOException {

    }

    public boolean isFolder(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        return entry != null && entry.isDirectory();
    }

    public boolean isFile(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        return entry != null && entry.isFile();
    }

    public boolean isSymbolicLink(PathHandle path) {
        return false;
    }

    public void setReadOnly(PathHandle of, boolean b) throws IOException {
        @Nullable Ext2Entry dir = getFsEntry(of);
        if (dir == null) return;
        dir.getAccessRights().setReadable(true, false);
        dir.getAccessRights().setWritable(!b, false);
        dir.getParent().flush();
    }

    public void setExecutable(PathHandle of, boolean b) throws IOException {
        @Nullable Ext2Entry dir = getFsEntry(of);
        if (dir == null) return;
        if (dir.isFile()) {
            dir.getAccessRights().setExecutable(b, false);
            dir.getParent().flush();
        } else {
            throw new IOException("PathHandle is not a file: " + of);
        }
    }

    public boolean isExecutable(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) return false;
        return entry.isFile() && entry.getAccessRights().canExecute();
    }

    public boolean isWritable(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) return false;
        return entry.isFile() && entry.getAccessRights().canWrite();
    }

    public boolean isReadable(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) return false;
        return entry.isFile() && entry.getAccessRights().canRead();
    }

    public int getOwner(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        return entry.getINode().getUid();
    }

    public int getGroup(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        return entry.getINode().getGid();
    }

    public int getPermissions(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        return entry.getINode().getMode();
    }

    public void setPermissions(PathHandle of, int mode) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        entry.getINode().setMode(mode);
        entry.getParent().flush();
    }

    public void setOwner(PathHandle of, int uid, int gid) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        entry.getINode().setUid(uid);
        entry.getINode().setGid(gid);
        entry.getParent().flush();
    }

    public void setGroup(PathHandle of, int gid) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        entry.getINode().setGid(gid);
        entry.getParent().flush();
    }

    public void setOwner(PathHandle of, int uid) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        entry.getINode().setUid(uid);
        entry.getParent().flush();
    }

    public long getGeneration(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        return entry.getINode().getGeneration();
    }

    public void setGeneration(PathHandle of, long generation) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        entry.getINode().setGeneration(generation);
        entry.getParent().flush();
    }

    public boolean isReadOnly(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) throw new IOException("PathHandle does not exist: " + of);
        return entry.getAccessRights().canRead() && !entry.getAccessRights().canWrite();
    }

    public boolean canWrite(PathHandle of) throws IOException {
        return isWritable(of);
    }

    public boolean canRead(PathHandle of) throws IOException {
        return isReadable(of);
    }

    public boolean canExecute(PathHandle of) throws IOException {
        Ext2Entry entry = getFsEntry(of);
        if (entry == null) return false;
        return entry.getAccessRights().canExecute();
    }

    public long lastModified(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return 0;
        return entry.getLastModified();
    }

    public long lastAccessed(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return 0;
        return entry.getLastAccessed();
    }

    public long creationTime(PathHandle path) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return 0;
        return entry.getINode().getCtime();
    }

    public void setLastAccessed(PathHandle path, long time) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return;
        entry.setLastAccessed(time);
        entry.getParent().flush();
    }

    public void setLastModified(PathHandle path, long time) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return;
        entry.setLastModified(time);
        entry.getParent().flush();
    }

    public void setCreationTime(PathHandle path, long time) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return;
        entry.getINode().setCtime(time);
        entry.getParent().flush();
    }

    public long getTotalSpace() throws IOException {
        return fs.getTotalSpace();
    }

    public long getUsableSpace() throws IOException {
        return fs.getUsableSpace();
    }

    public long getFreeSpace() throws IOException {
        return fs.getFreeSpace();
    }

    public void move(PathHandle source, PathHandle destination) throws IOException {
        Ext2Entry sourceEntry = getFsEntry(source);
        Ext2Entry destinationEntry = getFsEntry(destination);
        if (sourceEntry == null || destinationEntry == null) return;

        try (InputStream in = read(source)) {
            createFile(destination, in.readAllBytes());
        }

        sourceEntry.getParent().remove(sourceEntry.getName());
        sourceEntry.getParent().flush();
        destinationEntry.getParent().flush();
    }

    public void copy(PathHandle source, PathHandle destination) throws IOException {
        Ext2Entry sourceEntry = getFsEntry(source);
        Ext2Entry destinationEntry = getFsEntry(destination);
        if (sourceEntry == null || destinationEntry == null) return;

        try (InputStream in = read(source)) {
            createFile(destination, in.readAllBytes());
        }

        sourceEntry.getParent().flush();
        destinationEntry.getParent().flush();
    }

    public void write(PathHandle path, long offset, byte[] dataBytes) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return;

        if (entry.isFile()) {
            FSFile file = entry.getFile();
            long length = file.getLength();
            if (offset > length) throw new IOException("Offset out of range: " + offset);
            if (offset + dataBytes.length > length) {
                file.setLength(offset + dataBytes.length);
            }
            file.write(offset, ByteBuffer.wrap(dataBytes));
            file.flush();
            entry.getParent().flush();
        } else {
            throw new IOException("PathHandle is not a file: " + path);
        }
    }

    public void write(PathHandle path, byte[] dataBytes) throws IOException {
        write(path, 0, dataBytes);
    }

    public void truncate(PathHandle path, long size) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return;
        if (entry.isFile()) {
            entry.getFile().setLength(size);
        }
    }

    public void read(PathHandle path, ByteBuffer buffer, long offset) throws IOException {
        Ext2Entry entry = getFsEntry(path);
        if (entry == null) return;
        if (entry.isFile()) {
            entry.getFile().read(offset, buffer);
        }
    }

    public FileSystem<?> getFileSystem() {
        return fs;
    }

    private static class FSInputStream extends InputStream {
        private final FSFile file;
        private final ByteBuffer buffer = ByteBuffer.allocate(1024);
        private long bufferOffset;
        private long fileOffset = 0;

        private int markLimit;

        public FSInputStream(FSFile file, OpenOption[] options) throws IOException {
            this.file = file;
            for (OpenOption option : options) {
                if (option == StandardOpenOption.TRUNCATE_EXISTING) {
                    file.setLength(0);
                } else if (option != StandardOpenOption.READ) {
                    throw new UnsupportedOperationException("Option not supported: " + option);
                }
            }

            bufferOffset = 0;
            buffer.clear();
            if (fileOffset + buffer.capacity() > file.getLength()) {
                buffer.limit((int) (file.getLength() - fileOffset));
            }
            file.read(fileOffset, buffer);
            buffer.flip();
        }

        @Override
        public int read() throws IOException {
            if (bufferOffset == buffer.limit()) {
                bufferOffset = 0;
                buffer.clear();
                if (fileOffset + buffer.capacity() > file.getLength()) {
                    buffer.limit((int) (file.getLength() - fileOffset));
                }
                file.read(fileOffset, buffer);
                fileOffset += buffer.capacity();
            }

            return buffer.get() & 0xFF;
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) throws IOException {
            int bytesRead = 0;
            if (fileOffset >= file.getLength()) {
                return -1;
            }
            while (bytesRead < len) {
                if (fileOffset >= file.getLength()) {
                    return bytesRead;
                }

                int remaining = buffer.remaining();
                int len0 = Math.min(len - bytesRead, remaining);
                if (len0 == 0) {
                    return -1;
                }
                int fileRemaining = (int) (file.getLength() - fileOffset);
                int len1 = Math.min(len0, fileRemaining);
                buffer.get(b, off + bytesRead, len1);
                bufferOffset += len1;
                bytesRead += len1;
                fileOffset += len1;
                if (bufferOffset == buffer.capacity()) {
                    bufferOffset = 0;
                    buffer.clear();
                    if (fileOffset + buffer.capacity() > file.getLength()) {
                        buffer.limit((int) (file.getLength() - fileOffset));
                    }
                    file.read(fileOffset, buffer);
                }

                if (len0 < remaining) {
                    break;
                }
            }
            return bytesRead;
        }

        @Override
        public int read(byte @NotNull [] b) throws IOException {
            return read(b, 0, b.length);
        }
    }

    private static class DirNameIterator implements Iterator<String> {
        private final Iterator<FSEntry> iterator;

        public DirNameIterator(Ext2Directory dir) throws IOException {
            iterator = dir.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            return iterator.next().getName();
        }
    }

    private class FSOutputStream extends OutputStream {
        private final FSFile file;
        private final ByteBuffer buffer = ByteBuffer.allocate(1024);
        private boolean sync = false;
        private long bufferOffset = 0;
        private long fileOffset = 0;

        public FSOutputStream(FSFile file, OpenOption[] options) throws IOException {
            this.file = file;
            for (OpenOption option : options) {
                if (option == StandardOpenOption.TRUNCATE_EXISTING) {
                    file.setLength(0);
                } else if (option == StandardOpenOption.APPEND) {
                    fileOffset = file.getLength();
                } else if (option == StandardOpenOption.SYNC) {
                    sync = true;
                } else if (option != StandardOpenOption.WRITE && option != StandardOpenOption.CREATE && option != StandardOpenOption.CREATE_NEW) {
                    throw new UnsupportedOperationException("Option not supported: " + option);
                }
            }
        }

        @Override
        public void write(int b) throws IOException {
            buffer.put((byte) b);
            bufferOffset++;
            if (bufferOffset == buffer.capacity()) {
                buffer.flip();
                file.write(fileOffset, buffer);
                buffer.clear();
                bufferOffset = 0;
                fileOffset += buffer.capacity();
            }
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void flush() throws IOException {
            file.flush();
            if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
        }

        @Override
        public void close() throws IOException {
            file.flush();
            if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
        }
    }

    private class FSByteChannel implements SeekableByteChannel {
        private final PathHandle path;
        private final FSEntry entry;
        private final FSFile file;
        private final Set<OpenOption> options;
        private long offset = 0L;

        public FSByteChannel(PathHandle path, FSEntry entry, FSFile file, OpenOption... options) {
            this(path, entry, file, new HashSet<>(Arrays.asList(options)));
        }

        public FSByteChannel(PathHandle path, FSEntry entry, FSFile file, Set<OpenOption> options) {
            this.path = path;
            this.entry = entry;
            this.file = file;
            this.options = options;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            checkLock();
            if (!options.contains(StandardOpenOption.READ)) {
                throw new IOException("File isn't readable");
            }
            int position = dst.position();
            int blockSize = ((Ext2FileSystem) fs).getBlockSize();
            int readBytes = 0;
            while (dst.remaining() > blockSize) {
                file.read(offset, dst.slice(dst.position(), blockSize));
                dst.position(position += blockSize);
                offset += blockSize;
                readBytes += blockSize;
            }

            int len = dst.remaining();
            if (len > 0) {
                file.read(offset, dst);
                dst.position(position + len);
                offset += len;
                readBytes += len;
            }
            return readBytes;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            checkLock();
            if (options.contains(StandardOpenOption.APPEND)) {
                offset = file.getLength();
                int position = src.position();
                file.write(offset, src);
                offset += src.position() - position;
                return src.position() - position;
            }

            if (!options.contains(StandardOpenOption.WRITE)) {
                throw new IOException("File isn't writable");
            }
            int writeBytes = 0;
            int blockSize = ((Ext2FileSystem) fs).getBlockSize();
            int srcPos = src.position();
            while (src.remaining() > blockSize) {
                file.write(offset, src.slice(src.position(), blockSize));
                file.flush();
                src.position(srcPos += blockSize);
                offset += blockSize;
                writeBytes += blockSize;
            }

            int len = src.remaining();
            if (len > 0) {
                file.write(offset, src);
                file.flush();
                offset += len;
                writeBytes += len;
            }

            return writeBytes;
        }

        @Override
        public long position() throws IOException {
            checkLock();
            return offset;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            checkLock();
            offset = newPosition;
            return this;
        }

        @Override
        public long size() throws IOException {
            checkLock();
            return file.getLength();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            file.setLength(size);
            return this;
        }

        @Override
        public boolean isOpen() {
            return (!options.contains(StandardOpenOption.WRITE) || exclusiveLocks.contains(path)) && (!options.contains(StandardOpenOption.READ) || sharedLocks.containsKey(path));
        }

        @Override
        public void close() throws IOException {
            checkLock();
            file.flush();
            if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
            unlockPath(path, options);
        }

        private void checkLock() throws java.nio.file.FileSystemException {
            if (options.contains(StandardOpenOption.WRITE) && !exclusiveLocks.contains(path))
                throw new java.nio.file.FileSystemException(path.toString(), null, "Lost file lock");
            if (options.contains(StandardOpenOption.READ) && !sharedLocks.containsKey(path))
                throw new java.nio.file.FileSystemException(path.toString(), null, "Lost file lock");
        }
    }
}
