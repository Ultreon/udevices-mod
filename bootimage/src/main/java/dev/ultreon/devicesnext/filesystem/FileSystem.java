package dev.ultreon.devicesnext.filesystem;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class FileSystem {
    private final Disk disk;
    private final FSHeader fsHeader;
    private FSRoot root;
    private BitSet allocatedBlocks;

    public FileSystem(Disk disk) {
        this.disk = disk;
        allocatedBlocks = new BitSet((int) (disk.length() / Disk.BLOCK_SIZE));
        loadAllocations();

        this.fsHeader = new FSHeader(disk, this);

        if (fsHeader.isInitialized()) {
            this.root = new FSRoot(disk, this);
            this.root.open();
        }
    }

    public boolean isInitialized() {
        return this.fsHeader.isInitialized();
    }

    public void initialize() {
        this.flush();

        this.fsHeader.initialize();
        this.root = new FSRoot(disk, this);
        this.root.init();
        this.root.setLastAccessed(System.nanoTime());
        this.root.setLastModified(System.nanoTime());
        this.root.setCreated(System.nanoTime());
        this.root.flush();
        this.root.open();
    }

    public FSNode get(String path) {
        if (this.root == null) {
            throw new IllegalStateException("File system is not initialized");
        }

        if (path.equals("/")) {
            return this.root;
        }

        String replace = path.replace('\\', '/');
        if (!replace.startsWith("/")) {
            throw new IllegalArgumentException("Path must be absolute: " + replace);
        }

        FSNode node = this.root;
        String[] split = path.substring(1).split("/");
        for (String name : split) {
            if (!node.equals(root)) node.open();
            node = node.getChild(name);
            if (node == null) {
                return null;
            }
            if (!node.equals(root)) node.close();
        }

        return node;
    }

    public FSNode root() {
        return this.root;
    }

    public boolean isValid() {
        return this.root != null;
    }

    public void close() {
        this.root.close();
        this.root = null;
    }

    public int allocateBlock() {
        int i = allocatedBlocks.nextClearBit(1);
        if (i == -1) {
            throw new FileSystemIoException("Out of disk space");
        }
        allocatedBlocks.set(i);
        return i;
    }

    public void freeBlock(int block) {
        allocatedBlocks.clear(block);
    }

    public void flush() {
        long length = disk.length();
        long endAddress = Math.floorDiv(length, Disk.BLOCK_SIZE) * Disk.BLOCK_SIZE - Disk.BLOCK_SIZE * 4;
        long startAddress = (endAddress - allocatedBlocks.length() / Byte.SIZE);
        startAddress = Math.floorDiv(startAddress, Disk.BLOCK_SIZE) * Disk.BLOCK_SIZE;

        ByteBuffer buffer = ByteBuffer.allocate(Disk.BLOCK_SIZE);
        buffer.flip();
        buffer.position(0);
        buffer.limit(Disk.BLOCK_SIZE);
        long[] longArray = allocatedBlocks.toLongArray();
        for (long i = startAddress; i < endAddress; i += Disk.BLOCK_SIZE) {
            buffer.clear();
            buffer.position(0);
            for (int j = 0; j < Disk.BLOCK_SIZE / Long.BYTES; j++) {
                int block = (int) ((i - startAddress) / Disk.BLOCK_SIZE / Long.BYTES) + j;
                if (block >= longArray.length) {
                    buffer.putLong(0L);
                } else {
                    buffer.putLong(longArray[block]);
                }
            }
            disk.writeBlock((int) Math.floorDiv(i, Disk.BLOCK_SIZE), buffer, 0, Disk.BLOCK_SIZE);
        }

        disk.flush();
    }

    private void loadAllocations() {
        long length = disk.length();
        long endAddress = Math.floorDiv(length, Disk.BLOCK_SIZE) * Disk.BLOCK_SIZE;
        long startAddress = (endAddress - allocatedBlocks.length()) / Byte.SIZE;
        startAddress = Math.floorDiv(startAddress, Disk.BLOCK_SIZE) * Disk.BLOCK_SIZE;

        ByteBuffer buffer = ByteBuffer.allocate(Disk.BLOCK_SIZE);
        long[] longArray = new long[(int) (disk.length() / Disk.BLOCK_SIZE / Long.BYTES)];
        main: for (long i = startAddress; i < endAddress; i += Disk.BLOCK_SIZE) {
            for (int j = 0; j < Disk.BLOCK_SIZE / Long.BYTES; j++) {
                if (i > allocatedBlocks.length()) {
                    break main;
                }
                buffer.putLong(longArray[j]);
            }
            disk.readBlock((int) Math.floorDiv(i, Disk.BLOCK_SIZE), buffer, 0, Disk.BLOCK_SIZE);
        }

        allocatedBlocks = BitSet.valueOf(longArray);
    }

    public boolean exists(String of) {
        return get(of) != null;
    }

    private static class FSHeader {
        private final boolean initialized;
        private final Disk disk;
        private final FileSystem fs;

        public FSHeader(Disk disk, FileSystem fs) {
            this.disk = disk;
            this.fs = fs;
            ByteBuffer buffer = ByteBuffer.allocate(Disk.BLOCK_SIZE);
            disk.readBlock(0, buffer);
            buffer.flip();

            byte[] dst = new byte[16];
            buffer.get(0, dst);
            String signature = new String(dst);
            this.initialized = signature.equals("MineOSFS");
        }

        public boolean isInitialized() {
            return initialized;
        }

        public void initialize() {
            int i = fs.allocateBlock();
            ByteBuffer buffer = ByteBuffer.allocate(Disk.BLOCK_SIZE);
            buffer.put("MineOSFS".getBytes());
            buffer.flip();
            disk.writeBlock(0, buffer, 0, Disk.BLOCK_SIZE);
        }
    }
}
