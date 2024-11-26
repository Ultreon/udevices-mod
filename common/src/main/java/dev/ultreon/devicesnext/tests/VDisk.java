package dev.ultreon.devicesnext.tests;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VDisk {
    public static final int BLOCK_MAP_START = Integer.BYTES;
    public static final int MAX_BLOCKS = 8192;

    public static final String OUT_OF_DISK_OR_REGION_SPACE = "Out of disk or region space";
    public static final String OUT_OF_DISK_SPACE = "Out of disk space";
    public static final String DIRECTORY_IS_FULL = "Directory is full";

    private final RandomAccessFile io;
    private final Path filePath;
    private final int dataStart;
    private int blockSize = 512;
    private DirectoryNode root;



    private VDisk(Path filePath) throws IOException {
        this.filePath = filePath;
        this.io = new RandomAccessFile(filePath.toFile(), "rw");

        this.dataStart = getBlockCount() / 8 + Integer.BYTES;
    }

    public static VDisk open(Path filePath) throws IOException {
        VDisk vDisk = new VDisk(filePath);
        vDisk.loadFileTable();
        return vDisk;
    }

    public static VDisk format(Path filePath, long diskSize) throws IOException {
        if (!filePath.toFile().exists()) {
            Files.createFile(filePath);
            SeekableByteChannel seekableByteChannel = Files.newByteChannel(filePath, StandardOpenOption.WRITE);
            seekableByteChannel.position(diskSize - 1);
            seekableByteChannel.write(ByteBuffer.allocate(1));
            seekableByteChannel.close();
        }

        VDisk vDisk = new VDisk(filePath);
        vDisk.initialize();
        return vDisk;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void close() {
        try {
            io.close();
        } catch (Exception ignored) {
            // ignored
        }
    }

    private void loadFileTable() {

    }

    private int @Nullable [] findFreeBlocks(int count) throws IOException {
        io.seek(BLOCK_MAP_START);
        int[] blocks = new int[count];
        int idx = 0;
        for (int i = 8; i < getBlockCount(); i += 32) {
            int i1 = io.readInt();
            if (8 - Integer.bitCount(i1) <= 0) continue;
            while (Integer.bitCount(i1) < 8) {
                // Get first zero bit in integer.
                int index = Integer.numberOfTrailingZeros(Integer.highestOneBit(i1));
                if (index == 32) index = -1;
                int bit = 1 << index+1;
                i1 |= bit;

                blocks[idx] = i * 32 + index;
                idx++;
                if (idx == count) return blocks;
            }
        }

        return null;
    }

    private void allocateBlocks(int[] blocks) throws IOException {
        io.seek(BLOCK_MAP_START);
        for (int block : blocks) {
            if (block >= getBlockCount()) throw new IOException(OUT_OF_DISK_SPACE);
            io.seek(BLOCK_MAP_START + block / 32);
            int b = io.readInt();
            if ((b & (1 << (block % 32))) != 0) throw new IOException("Block is already allocated: " + Integer.toHexString(block));
            io.seek(BLOCK_MAP_START + block / 8);
            io.write(b | (1 << (block % 8)));
        }
    }

    private void deallocateBlocks(int[] blocks) throws IOException {
        io.seek(BLOCK_MAP_START);
        for (int block : blocks) {
            if (block >= getBlockCount()) throw new IOException(OUT_OF_DISK_SPACE);
            io.seek(BLOCK_MAP_START + block / 8);
            int b = io.readByte();
            if ((b & (1 << (block % 8))) == 0) throw new IOException("Block is not allocated: " + Integer.toHexString(block));
            io.seek(BLOCK_MAP_START + block / 8);
            io.write(b & ~(1 << (block % 8)));
        }
    }

    private void initialize() throws IOException {
        writeBlockUsageTable();

        long created = System.nanoTime();
        DirectoryNode root = new DirectoryNode("/", dataStart, created, created, created, 0777);
        io.seek(dataStart);
        root.write(this, io);

        this.root = root;
    }

    private void writeBlockUsageTable() throws IOException {
        long length = io.length();
        int blocks = getBlockCount(length);
        io.seek(0);
        io.writeInt(blocks);

        // Write bitflags
        for (int i = 0; i < blocks; i += 8) {
            io.write(0);
        }
    }

    private int getBlockCount() throws IOException {
        return (int) Math.ceil((double) io.length() / blockSize);
    }

    private int getBlockCount(long length) {
        return (int) Math.ceil((double) length / blockSize);
    }

    private int blockSize() {
        return blockSize;
    }

    public long size() throws IOException {
        return Files.size(filePath);
    }
    public void createFile(String path, byte[] data) throws IOException {
        if (!path.startsWith("/")) throw new IllegalArgumentException("Path must start with /");

        String substring = path.substring(1);
        String[] split = substring.split("/");
        String[] strings = Arrays.copyOfRange(split, 0, split.length - 1);
        String fileName = split[split.length - 1];

        for (String s : strings) {
            root = root.getDirectory(s);
            if (root == null) throw new IOException("Directory not found: " + path);
        }

        FileNode file = root.getFile(fileName);
        if (file == null) {
            FileNode file1 = root.createFile(this, fileName, 0);
            file1.writeData(this, io, data);
        } else {
            throw new FileAlreadyExistsException("File already exists: " + path);
        }
    }

    public byte[] readFile(String path) throws IOException {
        if (!path.startsWith("/")) throw new IllegalArgumentException("Path must start with /");

        String substring = path.substring(1);
        String[] split = substring.split("/");
        String[] strings = Arrays.copyOfRange(split, 0, split.length - 1);
        String fileName = split[split.length - 1];

        for (String s : strings) {
            root = root.getDirectory(s);
            if (root == null) throw new IllegalArgumentException("Directory not found: " + path);
        }
        FileNode file = root.getFile(fileName);
        if (file == null) throw new IllegalArgumentException("File not found: " + path);
        file.read(this, io);

        return file.readData(this, blockSize);
    }

    public void writeFile(String path, byte[] data) throws IOException {
        if (!path.startsWith("/")) throw new IllegalArgumentException("Path must start with /");

        String substring = path.substring(1);
        String[] split = substring.split("/");
        String[] strings = Arrays.copyOfRange(split, 0, split.length - 1);
        String fileName = split[split.length - 1];

        for (String s : strings) {
            root = root.getDirectory(s);
            if (root == null) throw new IllegalArgumentException("Directory not found: " + path);
        }
        FileNode file = root.getFile(fileName);
        if (file == null) throw new IllegalArgumentException("File not found: " + path);

        file.writeData(this, io, data);
    }

    private interface FSObject {


        void write(VDisk vDisk, DataOutput io) throws IOException;
        void read(VDisk vDisk, DataInput io) throws IOException;
    }
    private static class FSNode implements FSObject {
        public static final int TYPE_DIRECTORY = 0;

        public static final int TYPE_FILE = 1;
        public static final int TYPE_LINK = 2;
        private String name;
        protected int type;
        protected long size;
        protected long lastModified;
        protected long lastAccessed;

        protected long created;

        protected int flags;

        protected int[] blocks = new int[0];



        public FSNode(String name, int type, long size, long lastModified, long lastAccessed, long created, int flags) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.lastModified = lastModified;
            this.lastAccessed = lastAccessed;
            this.created = created;
            this.flags = flags;
        }

        @Override
        public void write(VDisk vDisk, DataOutput io) throws IOException {
            io.writeUTF(name);
            io.writeInt(type);
            io.writeLong(size);
            io.writeLong(lastModified);
            io.writeLong(lastAccessed);
            io.writeInt(flags);
        }

        @Override
        public void read(VDisk vDisk, DataInput io) throws IOException {
            name = io.readUTF();
            type = io.readInt();
            size = io.readLong();
            lastModified = io.readLong();
            lastAccessed = io.readLong();
            flags = io.readInt();
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public long getSize() {
            return size;
        }

        public long getLastModified() {
            return lastModified;
        }

        public long getLastAccessed() {
            return lastAccessed;
        }

        public void setLastAccessed(long lastAccessed) {
            this.lastAccessed = lastAccessed;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }
        public int getFlags() {
            return flags;
        }
    }
    private static final class FSRef implements FSObject {

        private int ptr;



        private FSRef(int ptr) {
            this.ptr = ptr;
        }

        @Override
        public void write(VDisk vDisk, DataOutput io) throws IOException {
            io.writeInt(ptr);
        }

        @Override
        public void read(VDisk vDisk, DataInput io) throws IOException {
            ptr = io.readInt();
        }

        public int ptr() {
            return ptr;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (FSRef) obj;
            return this.ptr == that.ptr;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ptr);
        }

        @Override
        public String toString() {
            return "FSRef[" +
                   "ptr=" + ptr + ']';
        }

    }
    private DataInput read(int[] blocks, DataInput io) {
        InputStream in = new BlockInputStream(this, io, blocks);
        return new DataInputStream(in);
    }
    private DataOutput write(int[] blocks, DataOutput io) {
        OutputStream out = new BlockOutputStream(this, io, blocks);
        return new DataOutputStream(out);
    }
    private static class DirectoryNode extends FSNode {
        private final Map<String, FSRef> entries = new HashMap<>();
        private static final int MAX_ENTRIES = 256;

        private final int ptr;



        public DirectoryNode(String name, int ptr, long lastModified, long lastAccessed, long created, int flags) {
            super(name, TYPE_DIRECTORY, 0, lastModified, lastAccessed, created, flags);
            this.ptr = ptr;
        }

        public void alloc(VDisk vDisk) throws IOException {
            this.blocks = vDisk.findFreeBlocks(4);

            write(vDisk, vDisk.io);
        }

        @Override
        public void read(VDisk vDisk, DataInput io) throws IOException {
            vDisk.io.seek(ptr);

            super.read(vDisk, io);

            DataInput read = vDisk.read(blocks, vDisk.io);

            for (int i = 0; i < MAX_ENTRIES; i++) {
                FSRef ref = new FSRef(read.readInt());
                String name = read.readUTF();
                entries.put(name, ref);
            }
        }

        @Override
        public void write(VDisk vDisk, DataOutput out) throws IOException {
            vDisk.io.seek(ptr);
            long end = ptr + 256;

            super.write(vDisk, out);

            if (entries.isEmpty()) return;
            flush(vDisk);
        }

        public void addEntry(VDisk vDisk, String name, FSRef entry) throws IOException {
            if (entries.size() + 1 >= MAX_ENTRIES) throw new IOException(DIRECTORY_IS_FULL);
            entries.put(name, entry);
            flush(vDisk);
        }

        public void removeEntry(VDisk vDisk, String name) throws IOException {
            entries.remove(name);
            flush(vDisk);
        }

        public DirectoryNode createDirectory(VDisk vDisk, String name) throws IOException {
            if (entries.size() + 1 >= MAX_ENTRIES) throw new IOException(DIRECTORY_IS_FULL);
            int[] freeBlocks = vDisk.findFreeBlocks(1);
            if (freeBlocks == null) throw new IOException(OUT_OF_DISK_SPACE);
            int block = freeBlocks[0];
            long nano = System.nanoTime();
            DirectoryNode directoryNode = new DirectoryNode(name, block * vDisk.blockSize(), nano, nano, nano, 0);
            entries.put(name, new FSRef(directoryNode.ptr));
            directoryNode.write(vDisk, vDisk.io);
            return directoryNode;
        }

        public FileNode createFile(VDisk vDisk, String name, long size) throws IOException {
            if (entries.size() + 1 >= MAX_ENTRIES) throw new IOException(DIRECTORY_IS_FULL);
            int[] freeBlocks = vDisk.findFreeBlocks(1);
            if (freeBlocks == null) throw new IOException(OUT_OF_DISK_SPACE);
            int block = freeBlocks[0];
            long nano = System.nanoTime();
            FileNode fileNode = new FileNode(vDisk, name, block * vDisk.blockSize(), nano, nano, nano, 0, size);
            entries.put(name, new FSRef(fileNode.ptr));
            fileNode.write(vDisk, vDisk.io);
            return fileNode;
        }

        public void flush(VDisk vDisk) throws IOException {
            if (entries.size() > MAX_ENTRIES) throw new IOException(DIRECTORY_IS_FULL);

            DataOutput write = vDisk.write(blocks, vDisk.io);

            for (Map.Entry<String, FSRef> entry : entries.entrySet()) {
                write.writeUTF(entry.getKey());
                write.writeInt(entry.getValue().ptr());
            }
        }
        public DirectoryNode getDirectory(String s) {
            FSRef ref = entries.get(s);
            if (ref == null) return null;
            return new DirectoryNode(s, ref.ptr, lastModified, lastAccessed, created, flags);
        }
        public FileNode getFile(String s) {
            FSRef ref = entries.get(s);
            if (ref == null) return null;
            return new FileNode(s, ref.ptr, lastModified, lastAccessed, created, flags);
        }
    }
    private static class FileNode extends FSNode {
        private static final String FILE_IS_TOO_BIG = "File is too big";

        private final int ptr;

        public FileNode(String name, int ptr, long lastModified, long lastAccessed, long created, int flags) {
            super(name, TYPE_FILE, 0, lastModified, lastAccessed, created, flags);
            this.size = 0;
            this.ptr = ptr;
        }

        public FileNode(VDisk vDisk, String name, int ptr, long lastModified, long lastAccessed, long created, int flags, long size) throws IOException {
            super(name, TYPE_FILE, size, lastModified, lastAccessed, created, flags);
            this.size = size;
            this.ptr = ptr;

            int[] freeBlocks = vDisk.findFreeBlocks((int) (size / vDisk.blockSize() + 1));
            if (freeBlocks == null) throw new IOException(OUT_OF_DISK_SPACE);
            this.blocks = freeBlocks;
            vDisk.allocateBlocks(this.blocks);

            write(vDisk, vDisk.io);
        }

        @Override
        public void write(VDisk vDisk, DataOutput io) throws IOException {
            if (blocks.length > MAX_BLOCKS) throw new IOException(FILE_IS_TOO_BIG);
            vDisk.io.seek(ptr);

            super.write(vDisk, io);
            io.writeInt(blocks.length);
            for (int block : blocks) {
                io.writeInt(block);
            }

            this.lastModified = System.nanoTime();
        }

        @Override
        public void read(VDisk vDisk, DataInput io) throws IOException {
            vDisk.io.seek(ptr);

            super.read(vDisk, io);
            int numBlocks = io.readInt();
            if (numBlocks > MAX_BLOCKS) throw new IOException(FILE_IS_TOO_BIG);
            blocks = new int[numBlocks];
            for (int i = 0; i < numBlocks; i++) {
                blocks[i] = io.readInt();
            }

            this.lastAccessed = System.nanoTime();
        }

        private void addBlock(int block) {
            blocks = Arrays.copyOf(blocks, blocks.length + 1);
            blocks[blocks.length - 1] = block;
        }

        private void removeBlock(int block) {
            int[] newBlocks = new int[blocks.length - 1];
            int idx = 0;
            for (int b : blocks) {
                if (b != block) newBlocks[idx++] = b;
            }
            blocks = newBlocks;
        }

        public void writeData(VDisk vDisk, RandomAccessFile io, byte[] data) throws IOException {
            int offset = 0;
            int blockSize = vDisk.blockSize();
            int blocks = (int) Math.ceil((double) data.length / blockSize);
            if (this.blocks.length < blocks) {
                int[] freeBlocks = vDisk.findFreeBlocks(blocks - this.blocks.length);
                if (freeBlocks == null) throw new IOException(OUT_OF_DISK_SPACE);
                vDisk.allocateBlocks(freeBlocks);
                this.blocks = Arrays.copyOf(this.blocks, this.blocks.length + freeBlocks.length);
                System.arraycopy(freeBlocks, 0, this.blocks, this.blocks.length - freeBlocks.length, freeBlocks.length);

            }

            DataOutput write = vDisk.write(this.blocks, io);

            byte[] buffer = new byte[blockSize];
            while (offset < data.length) {
                int len = Math.min(data.length - offset, blockSize);
                System.arraycopy(data, offset, buffer, 0, len);
                write.write(buffer, 0, len);
                offset += len;
            }

            this.size = data.length;
            this.write(vDisk, vDisk.io);
        }

        public void readData(VDisk vDisk, RandomAccessFile io, byte[] data) throws IOException {
            int offset = 0;
            int blockSize = vDisk.blockSize();
            while (offset < data.length) {
                int len = Math.min(data.length - offset, blockSize);
                int block = blocks[offset / blockSize];
                int ptr = block * blockSize + vDisk.dataStart;
                io.seek(ptr);
                io.read(data, offset, len);
                offset += len;
            }
        }
        public byte[] readData(VDisk vDisk, int blockSize) throws IOException {
            byte[] data = new byte[(int) size];
            readData(vDisk, vDisk.io, data);
            return data;
        }

    }

    private static class BlockOutputStream extends OutputStream {
        private final VDisk vDisk;
        private final DataOutput io;
        private final int[] blocks;

        private int blockIdx = 0;
        private int blockOffset = 0;

        public BlockOutputStream(VDisk vDisk, DataOutput io, int[] blocks) {
            this.vDisk = vDisk;
            this.io = io;
            this.blocks = blocks;
        }

        @Override
        public void write(int b) throws IOException {
            if (blockIdx >= blocks.length) throw new IOException(OUT_OF_DISK_OR_REGION_SPACE);

            vDisk.io.seek((long) blocks[blockIdx] * vDisk.blockSize() + vDisk.dataStart + blockOffset);
            io.write(b);
            blockOffset++;
            if (blockOffset == vDisk.blockSize()) {
                blockOffset = 0;
                blockIdx++;
            } else if (blockIdx == blocks.length) {
                throw new IOException(OUT_OF_DISK_OR_REGION_SPACE);
            }
        }
    }

    private static class BlockInputStream extends InputStream {
        private final VDisk vDisk;
        private final DataInput io;
        private final int[] blocks;
        private int blockIdx = 0;
        private int blockOffset = 0;

        public BlockInputStream(VDisk vDisk, DataInput io, int[] blocks) {
            this.vDisk = vDisk;
            this.io = io;
            this.blocks = blocks;
        }

        @Override
        public int read() throws IOException {
            if (blockIdx >= blocks.length) return -1;

            vDisk.io.seek((long) blocks[blockIdx] * vDisk.blockSize() + vDisk.dataStart + blockOffset);
            byte b = io.readByte();
            blockOffset++;
            if (blockOffset == vDisk.blockSize()) {
                blockOffset = 0;
                blockIdx++;
            }
            return b & 0xFF;
        }
    }
}
