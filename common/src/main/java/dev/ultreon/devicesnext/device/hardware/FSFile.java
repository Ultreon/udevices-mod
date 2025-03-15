package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.mineos.Disk;
import dev.ultreon.devicesnext.mineos.FileSystem;
import dev.ultreon.devicesnext.mineos.FileSystemIoException;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static dev.ultreon.devicesnext.mineos.Disk.BLOCK_SIZE;

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
    long dataBlock = -1;
    long created = -1;
    long lastAccessed = -1;
    long lastModified = -1;
    int blockCount;
    int mode;
    private boolean opened = false;

    public FSFile(Disk disk, FileSystem fs, @NotNull FSDirectory parent, long address) {
        this.disk = disk;
        this.fs = fs;
        this.parent = parent;
        this.address = address;

        disk.readBlock(Math.floorDiv(address, BLOCK_SIZE), buffer);
        buffer.flip();
        buffer.position(0);

        byte b = buffer.get();
        byte[] dst = new byte[16];
        buffer.get(1, dst);
        if (b > dst.length) {
            UDevicesMod.LOGGER.warn("File name too long: " + b + " > " + dst.length);
            dst = new byte[b];
        }
        this.name = new String(dst, 0, b);
        buffer.position(17);

        this.length = buffer.getLong();
        this.dataBlock = buffer.getLong();
        disk.isWithinSpace(dataBlock);

        this.created = buffer.getLong();
        this.lastAccessed = buffer.getLong();
        this.lastModified = buffer.getLong();
        this.mode = buffer.getInt();

        buffer.clear();
    }

    public FSFile(Disk disk, FileSystem fs, FSDirectory parent) {
        this.disk = disk;
        this.fs = fs;
        this.parent = parent;
        this.address = (long) fs.allocateBlock() * BLOCK_SIZE;
        this.dataBlock = fs.allocateBlock();
        this.opened = true;
    }

    @Override
    public void open() {
        if (opened) return;

        disk.readBlock(dataBlock, buffer);
        buffer.flip();
        buffer.position(0);
        this.blockCount = buffer.getInt();
        if (blockCount > Disk.BLOCK_SIZE / Integer.BYTES) {
            throw new FileSystemIoException("File exceeds max size of " + (Disk.BLOCK_SIZE / Integer.BYTES) + " blocks");
        }
        for (int i = 0; i < blockCount; i++) {
            int block = buffer.getInt();
            this.blocks.add(block);
        }

        buffer.clear();
        opened = true;
    }

    public void read(long offset, ByteBuffer buffer) {
        if (!opened) throw new IllegalStateException("File node not opened!");

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
        if (!opened) throw new IllegalStateException("File node not opened!");
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
        opened = false;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
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
        return lastModified;
    }

    @Override
    public long getLastAccessed() {
        return lastAccessed;
    }

    @Override
    public long getCreated() {
        return created;
    }

    @Override
    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public void setCreated(long created) {
        this.created = created;
    }

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public void flush() {
        if (!opened) throw new IllegalStateException("File node not opened!");
        if (address == -1) return;

        buffer.clear();
        buffer.position(0);
        buffer.put((byte) name.length());
        buffer.put(name.getBytes());
        buffer.position(16);
        buffer.putLong(length);
        buffer.putLong(dataBlock);
        buffer.putLong(created);
        buffer.putLong(lastAccessed);
        buffer.putLong(lastModified);
        buffer.putInt(mode);
        buffer.flip();
        disk.writeBlock((int) Math.floorDiv(address, Disk.BLOCK_SIZE), buffer, 0, buffer.capacity());

        if (length != oldLength) {
            if (length > oldLength) {
                this.allocate(length - oldLength);
            } else {
                // TODO
                throw new UnsupportedOperationException("TODO");
            }
        }

        oldLength = length;

        buffer.clear();
        buffer.flip();
        buffer.limit(BLOCK_SIZE);
        buffer.position(0);

        buffer.putInt(blockCount);
        int blockIdx = 0;
        for (int block : blocks.intStream().sorted().toArray()) {
            buffer.putInt(block);
            blockIdx++;
        }
        buffer.flip();
        disk.writeBlock((int) Math.floorDiv(dataBlock, Disk.BLOCK_SIZE), buffer, 0, buffer.capacity());
        fs.flush();
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    private void allocate(long amount) {
        if (!opened) throw new IllegalStateException("File node not opened!");
        long remaining = (oldLength + amount) % Disk.BLOCK_SIZE;
        long blocks = Math.floorDiv(oldLength + amount - remaining, Disk.BLOCK_SIZE);
        for (long i = 0; i < blocks; i++) {
            this.blocks.add(fs.allocateBlock());
        }
        this.blockCount = (int) blocks;
    }

    public void rename(String name) {
        if (!opened) throw new IllegalStateException("File node not opened!");
        parent.rename(name, this);
        this.name = name;
        this.parent.flush();
        this.flush();
    }

    public void truncate(long length) {
        if (!opened) throw new IllegalStateException("File node not opened!");
        this.length = length;
        this.flush();
    }

    public void delete() {
        if (!opened) throw new IllegalStateException("File node not opened!");
        parent.delete(name);
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
