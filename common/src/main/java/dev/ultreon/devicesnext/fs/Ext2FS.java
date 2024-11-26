package dev.ultreon.devicesnext.fs;

import com.google.common.base.Preconditions;
import dev.ultreon.devicesnext.device.VirtualBlockDevice;
import dev.ultreon.devicesnext.device.hardware.MCHardDrive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.driver.virtual.VirtualDevice;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.*;
import org.jnode.fs.ext2.BlockSize;
import org.jnode.fs.ext2.Ext2FileSystem;
import org.jnode.fs.ext2.Ext2FileSystemFormatter;
import org.jnode.fs.ext2.Ext2FileSystemType;
import org.jnode.fs.spi.AbstractFileSystem;

import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Collections;
import java.util.Iterator;

public class Ext2FS implements FS {
    private final FileSystem<?> fs;

    private Ext2FS(Ext2FileSystem fs) throws IOException {
        this.fs = fs;
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

    public static Ext2FS format(Path filePath, long diskSize) throws IOException, FileSystemException {
        var device = new VirtualDevice("MineDisk");
        device.registerAPI(BlockDeviceAPI.class, new VirtualBlockDevice(filePath.toFile().getAbsolutePath(), diskSize));

        var blockDevice = new VirtualBlockDevice(filePath.toFile().getAbsolutePath(), diskSize);
        var formatter = new Ext2FileSystemFormatter(BlockSize._1Kb);
        var fs = formatter.format(device);
        return new Ext2FS(fs);
    }

    public static Ext2FS open(MCHardDrive drive) throws IOException, FileSystemException {
        var fs = new Ext2FileSystem(drive.getDevice(), false, new Ext2FileSystemType());
        fs.read();
        return new Ext2FS(fs);
    }

    public static Ext2FS open(MCHardDrive drive, boolean readOnly) throws IOException, FileSystemException {
        var fs = new Ext2FileSystem(drive.getDevice(), readOnly, new Ext2FileSystemType());
        fs.read();
        return new Ext2FS(fs);
    }

    public static Ext2FS format(MCHardDrive drive, long diskSize) throws IOException, FileSystemException {
        var formatter = new Ext2FileSystemFormatter(BlockSize._1Kb);
        var fs = formatter.format(drive.getDevice());
        fs.read();
        return new Ext2FS(fs);
    }

    @Override
    public void close() throws IOException {
        fs.close();
    }

    @Override
    public InputStream read(Path path, OpenOption... options) throws IOException {
        FSFile file = getFileAt(path);
        if (file == null) throw new NoSuchFileException("File not found: " + path);
        return new FSInputStream(file, options);
    }

    @Override
    public OutputStream write(Path path, OpenOption... options) throws IOException {
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
            FSDirectory parentDir = getDirectoryAt(path.getParent());
            if (parentDir == null) {
                throw new NoSuchFileException(path.getParent().toString(), null, "parent directory not found");
            }
            FSEntry entry = parentDir.getEntry(path.getFileName().toString());
            if (entry == null) {
                file = parentDir.addFile(path.getFileName().toString()).getFile();
            } else if (entry.isFile()) {
                file = entry.getFile();
            } else {
                throw new FileAlreadyExistsException("Directory already exists: " + path);
            }
        } else if (createNew) {
            FSDirectory parentDir = getDirectoryAt(path.getParent());
            if (parentDir == null) {
                throw new NoSuchFileException(path.getParent().toString(), null, "parent directory not found");
            }
            if (parentDir.getEntry(path.getFileName().toString()) != null) {
                throw new FileAlreadyExistsException("File already exists: " + path);
            }
            file = parentDir.addFile(path.getFileName().toString()).getFile();
        } else {
            file = getFileAt(path);
            if (file == null) throw new NoSuchFileException(path.toString(), null, "fdile not found");
        }
        return new FSOutputStream(file, options);
    }

    @Override
    public boolean exists(Path path) throws IOException {
        return getFileAt(path) != null;
    }

    private FSFile getFileAt(Path path) throws IOException {
        FSEntry entry = getFsEntry(path);
        if (entry == null) return null;
        return entry.getFile();
    }

    private FSDirectory getDirectoryAt(Path path) throws IOException {
        Preconditions.checkNotNull(path, "path");
        FSEntry fsEntry = getFsEntry(path);
        if (fsEntry == null) return null;
        if (!fsEntry.isDirectory()) throw new IllegalArgumentException("Path is not a directory: " + path);
        return fsEntry.getDirectory();
    }

    private @Nullable FSEntry getFsEntry(Path path) throws IOException {
        if (!path.isAbsolute()) throw new IllegalArgumentException("Path must be absolute");
        if (path.getParent() == null) return fs.getRootEntry();

        FSDirectory root = fs.getRootEntry().getDirectory();
        for (Path s : path.getParent()) {
            FSEntry entry = root.getEntry(s.toString());
            if (!entry.isDirectory()) return null;
            root = entry.getDirectory();
            if (root == null) return null;
        }
        return root.getEntry(path.getFileName().toString());
    }

    @Override
    public void flush() throws IOException {
        if (fs instanceof AbstractFileSystem<?>) ((Flushable) fs).flush();
    }

    @Override
    public void createFile(Path path, byte[] data) throws IOException {
        if (!path.isAbsolute()) throw new IllegalArgumentException("Path must be absolute");

        FSDirectory parentDir = path.getParent() == null ? fs.getRootEntry().getDirectory() : getDirectoryAt(path.getParent());
        if (parentDir == null) throw new NoSuchFileException(path.getParent().toString(), null, "parent directory not found");
        FSFile file = parentDir.addFile(path.getFileName().toString()).getFile();
        file.setLength(data.length);
        file.write(0, ByteBuffer.wrap(data));
        file.flush();
        if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
    }

    @Override
    public void createDirectory(Path path) throws IOException {
        if (!path.isAbsolute()) throw new IllegalArgumentException("Path must be absolute");
        FSDirectory parentDir = path.getParent() == null ? fs.getRootEntry().getDirectory() : getDirectoryAt(path.getParent());
        parentDir.addDirectory(path.getFileName().toString());
        if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
    }

    @Override
    public Iterator<String> listDirectory(Path of) throws IOException {
        FSDirectory dir = getDirectoryAt(of);
        if (dir == null) return Collections.emptyIterator();
        return new DirNameIterator(dir);
    }

    @Override
    public void delete(Path path) throws IOException {
        FSEntry parent = path.getParent() == null ? fs.getRootEntry() : getFsEntry(path.getParent());
        if (parent == null) return;

        if (!parent.isDirectory()) throw new NotDirectoryException("Path is not a directory: " + path);

        FSEntry entry = parent.getDirectory().getEntry(path.getFileName().toString());
        if (entry.isDirectory()) {
            if (entry.getDirectory().iterator().hasNext()) {
                throw new DirectoryNotEmptyException("Directory is not empty: " + path);
            }

            parent.getDirectory().remove(path.getFileName().toString());
            return;
        }

        if (!entry.isFile()) throw new IOException("Path is not a file: " + path);
        parent.getDirectory().remove(path.getFileName().toString());
    }

    @Override
    public long size(Path path) throws IOException {
        FSEntry entry = getFsEntry(path);
        if (entry == null) return 0;
        if (!entry.isFile()) throw new IOException("Path is not a file: " + path);
        return entry.getFile().getLength();
    }

    @Override
    public void rename(Path from, String name) throws IOException {
        if (name.contains("/")) throw new IOException("Invalid name: " + name);

        FSEntry parent = from.getParent() == null ? fs.getRootEntry() : getFsEntry(from.getParent());
        if (parent == null) return;

        if (!parent.isDirectory()) throw new NotDirectoryException("Path is not a directory: " + from);

        FSEntry entry = parent.getDirectory().getEntry(from.getFileName().toString());
        if (entry == null) return;
        entry.setName(name);
        parent.getDirectory().flush();
    }

    private static class FSInputStream extends InputStream {
        private final FSFile file;
        private final ByteBuffer buffer = ByteBuffer.allocate(1024);
        private long bufferOffset = 0;
        private long fileOffset = 0;

        private long markOffset = 0;
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
        private final Iterator<? extends FSEntry> iterator;

        public DirNameIterator(FSDirectory dir) throws IOException {
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
            if (bufferOffset == buffer.limit()) {
                file.setLength(fileOffset + 1);

                bufferOffset = 0;
                buffer.flip();
                file.write(fileOffset, buffer);
                buffer.clear();
                fileOffset++;
                if (sync) {
                    file.flush();
                    if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
                }
            }
        }

        @Override
        public void write(byte @NotNull [] b, int off, int len) throws IOException {
            for (int i = 0; i < len; i += buffer.capacity()) {
                int writeLen = Math.min(buffer.capacity(), len - i);
                buffer.put(b, off + i, writeLen);
                bufferOffset += writeLen;
                if (bufferOffset == buffer.limit()) {
                    file.setLength(fileOffset + writeLen);

                    bufferOffset = 0;
                    buffer.flip();
                    file.write(fileOffset, buffer);
                    buffer.clear();
                    fileOffset += writeLen;
                    if (sync) {
                        file.flush();
                        if (fs instanceof AbstractFileSystem<?> absFs) absFs.flush();
                    }
                }
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
}
