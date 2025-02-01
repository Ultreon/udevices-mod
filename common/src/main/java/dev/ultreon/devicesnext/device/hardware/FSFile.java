package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.mineos.Disk;
import dev.ultreon.devicesnext.mineos.FileSystem;
import dev.ultreon.devicesnext.mineos.FileSystemIoException;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class FSFile implements FSNode {
    private final Disk disk;
    private final FileSystem fs;
    private final FSDirectory parent;
    private final long address;
    private final ByteBuffer buffer = ByteBuffer.allocate(Disk.BLOCK_SIZE);
    private final IntSet blocks = IntArraySet.of();
    private long oldLength = -1;
    String name = "";
    long length = -1;
    long dataAddress = -1;
    long created = -1;
    long lastAccessed = -1;
    long lastModified = -1;
    int blockCount;
    int mode;

    public FSFile(Disk disk, FileSystem fs, @NotNull FSDirectory parent, long address) {
        this.disk = disk;
        this.fs = fs;
        this.parent = parent;
        this.address = address;

        disk.readBlock(Math.floorDiv(address, Disk.BLOCK_SIZE), buffer);
        buffer.flip();

        byte[] dst = new byte[16];
        buffer.get(0, dst);
        this.name = new String(dst);

        int idx = 16;
        this.length = buffer.getLong(idx += Long.BYTES);
        this.dataAddress = buffer.getLong(idx += Long.BYTES);
        this.created = buffer.getLong(idx += Long.BYTES);
        this.lastAccessed = buffer.getLong(idx += Long.BYTES);
        this.lastModified = buffer.getLong(idx += Long.BYTES);
        this.mode = buffer.getInt();

        buffer.clear();
    }

    @Override
    public void open() {
        disk.readBlock(Math.floorDiv(dataAddress, Disk.BLOCK_SIZE), buffer);
        this.blockCount = buffer.getInt();
        if (blockCount > Disk.BLOCK_SIZE / Integer.BYTES) {
            throw new FileSystemIoException("File exceeds max size of " + (Disk.BLOCK_SIZE / Integer.BYTES) + " blocks");
        }
        for (int i = 0; i < blockCount; i++) {
            int block = buffer.getInt();
            this.blocks.add(block);
        }

        buffer.clear();
    }

    public void read(long offset, ByteBuffer buffer) {
        int capacity = buffer.capacity();
        if (offset + capacity > this.length) {
            throw new FileSystemIoException("Offset " + (offset + capacity) + " exceeds file length " + this.length);
        }
        if (blockForOffset(offset) == blockForOffset(offset + capacity)) {
            disk.readBlock(blockForOffset(offset), buffer, Math.floorMod(offset, Disk.BLOCK_SIZE), capacity);
            return;
        }

        int blockOffset = Math.floorMod(offset, Disk.BLOCK_SIZE);
        int blockOffsetEnd = Math.floorMod(offset + capacity, Disk.BLOCK_SIZE);

        // Read the first block up to the end of the first block.
        disk.readBlock(blockForOffset(offset), buffer, blockOffset, Disk.BLOCK_SIZE - blockOffset);

        // Read the rest of the blocks.
        for (int i = blockForOffset(offset) + 1; i < blockForOffset(offset + capacity); i++) {
            disk.readBlock(i, buffer, 0, Disk.BLOCK_SIZE);
        }

        // Read the last block up to the end of the last block.
        disk.readBlock(blockForOffset(offset + capacity), buffer, 0, blockOffsetEnd);
    }

    public void write(long offset, ByteBuffer buffer) {
        int capacity = buffer.capacity();
        if (offset + capacity > this.length) {
            throw new FileSystemIoException("Offset " + (offset + capacity) + " exceeds file length " + this.length);
        }
        if (blockForOffset(offset) == blockForOffset(offset + capacity)) {
            disk.writeBlock(blockForOffset(offset), buffer, Math.floorMod(offset, Disk.BLOCK_SIZE), capacity);
            return;
        }

        int blockOffset = Math.floorMod(offset, Disk.BLOCK_SIZE);
        int blockOffsetEnd = Math.floorMod(offset + capacity, Disk.BLOCK_SIZE);

        // Write the first block up to the end of the first block.
        disk.writeBlock(blockForOffset(offset), buffer, blockOffset, Disk.BLOCK_SIZE - blockOffset);

        // Write the rest of the blocks.
        for (int i = blockForOffset(offset) + 1; i < blockForOffset(offset + capacity); i++) {
            disk.writeBlock(i, buffer, 0, Disk.BLOCK_SIZE);
        }

        // Write the last block up to the end of the last block.
        disk.writeBlock(blockForOffset(offset + capacity), buffer, 0, blockOffsetEnd);
    }

    private int blockForOffset(long offset) {
        return (int) Math.floorDiv(offset, Disk.BLOCK_SIZE);
    }

    @Override
    public void close() {
        this.name = "";
        this.length = -1;
        this.dataAddress = -1;
        this.created = -1;
        this.lastAccessed = -1;
        this.lastModified = -1;
        this.blockCount = 0;
        this.mode = 0;
    }

    public long getLength() {
        return length;
    }

    public long setLength(long length) {
        this.length = length;
        return length;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @NotNull FSDirectory getParent() {
        return parent;
    }

    @Override
    public FSNode getChild(String name) {
        return null;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public long getLastAccessed() {
        return 0;
    }

    @Override
    public long getCreated() {
        return 0;
    }

    @Override
    public void setLastAccessed(long lastAccessed) {

    }

    @Override
    public void setLastModified(long lastModified) {

    }

    @Override
    public void setCreated(long created) {

    }

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public void flush() {
        if (address == -1) return;

        buffer.clear();
        buffer.put(name.getBytes());

        buffer.putLong(length);
        buffer.putLong(dataAddress);
        buffer.putLong(created);
        buffer.putLong(lastAccessed);
        buffer.putLong(lastModified);
        buffer.putInt(mode);
        buffer.flip();
        disk.writeBlock((int) Math.floorDiv(address, Disk.BLOCK_SIZE), buffer, 0, buffer.capacity());

        if (length != oldLength) {
            if (length > oldLength) {
                this.allocate(length - oldLength);
            }
        }

        oldLength = length;

        buffer.clear();
        buffer.flip();

        buffer.putInt(blockCount);
        int blockIdx = 0;
        for (int block : blocks.intStream().sorted().toArray()) {
            buffer.putInt(block);
            blockIdx++;
        }
        buffer.flip();
        disk.writeBlock((int) Math.floorDiv(dataAddress, Disk.BLOCK_SIZE), buffer, 0, buffer.capacity());
        fs.flush();
    }

    private void allocate(long amount) {
        long remaining = (oldLength + amount) % Disk.BLOCK_SIZE;
        long blocks = Math.floorDiv(oldLength + amount - remaining, Disk.BLOCK_SIZE);
        for (long i = 0; i < blocks; i++) {
            this.blocks.add(fs.allocateBlock());
        }
        this.blockCount = (int) blocks;
        this.flush();
    }

    public void rename(String name) {
        parent.rename(name, this);
        this.name = name;
        this.parent.flush();
        this.flush();
    }

    public void truncate(long length) {
        this.length = length;
        this.flush();
    }

    public void delete() {
        parent.delete(name);
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
