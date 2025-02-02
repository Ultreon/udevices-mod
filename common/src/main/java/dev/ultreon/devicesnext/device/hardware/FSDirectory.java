package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.mineos.Disk;
import dev.ultreon.devicesnext.mineos.FileSystem;
import dev.ultreon.devicesnext.mineos.FileSystemIoException;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static dev.ultreon.devicesnext.mineos.Disk.BLOCK_SIZE;

public class FSDirectory implements FSNode {
    private final Disk disk;
    private final FSDirectory parent;
    private final long address;
    private final ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
    private String name = "";
    private long length = -1;
    private long dataBlock = -1;
    private long created = -1;
    private long lastAccessed = -1;
    private long lastModified = -1;
    private final Map<String, FSNode> children = new HashMap<>();

    protected final FileSystem fs;

    public FSDirectory(Disk disk, FileSystem fs, FSDirectory parent, long address) {
        this.disk = disk;
        this.parent = parent;
        this.address = address;
        this.fs = fs;

        if (address < BLOCK_SIZE) {
            throw new IllegalArgumentException("Invalid address: " + address);
        }

        disk.readBlock(Math.floorDiv(address, BLOCK_SIZE), buffer);
        buffer.flip();

        byte b = buffer.get();
        byte[] dst = new byte[16];
        buffer.get(0, dst);
        if (b > dst.length) {
            UDevicesMod.LOGGER.warn("File name too long: " + b + " > " + dst.length);
            dst = new byte[b];
        }
        this.name = new String(dst, 0, b);
        buffer.position(16);
        this.length = buffer.getLong();
        this.dataBlock = buffer.getLong();
        disk.isWithinSpace(dataBlock);

        this.created = buffer.getLong();
        this.lastAccessed = buffer.getLong();
        this.lastModified = buffer.getLong();

        buffer.clear();
    }

    @Override
    public void open() {
        if (!children.isEmpty() && !(this instanceof FSRoot)) throw new FileSystemIoException("File in use");

        if (dataBlock > 0) {
            buffer.clear();
            disk.readBlock(dataBlock, buffer);
            buffer.flip();
            buffer.position(0);
            int childCount = buffer.getInt();
            children.clear();
            for (int i = 0; i < childCount; i++) {
                long childAddress = buffer.getLong();
                int childType = buffer.getInt();
                FSNode child = null;
                if (childType == 0) {
                    child = new FSDirectory(disk, fs, this, childAddress);
                } else if (childType == 1) {
                    child = new FSFile(disk, fs, this, childAddress);
                } else {
                    throw new UnsupportedOperationException("Unknown child type: " + childType);
                }
                children.put(child.getName(), child);
            }
            buffer.clear();
        } else {
            children.clear();
            flush();
        }
    }

    @Override
    public void close() {
        children.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable FSDirectory getParent() {
        return parent;
    }

    @Override
    public FSNode getChild(String name) {
        return children.get(name);
    }

    @Override
    public boolean isDirectory() {
        return true;
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

    public void delete(String name) {

    }

    public boolean isRoot() {
        return false;
    }

    public void createFile(String name) {
        FSFile file = new FSFile(disk, fs, this, (long) fs.allocateBlock() * BLOCK_SIZE);
        file.name = name;
        file.setCreated(System.nanoTime());
        file.setLastAccessed(System.nanoTime());
        file.setLastModified(System.nanoTime());
        file.setLength(0);
        file.setMode(0);
        file.flush();
        file.close();
        this.children.put(name, new FSFile(disk, fs, this, (long) fs.allocateBlock() * BLOCK_SIZE));

        flush();
        fs.flush();
    }

    public void createDirectory(String name) {
        FSDirectory value = new FSDirectory(disk, fs, this, (long) fs.allocateBlock() * BLOCK_SIZE);
        value.name = name;
        value.setCreated(System.nanoTime());
        value.setLastAccessed(System.nanoTime());
        value.setLastModified(System.nanoTime());
        value.flush();
        value.close();

        this.children.put(name, value);
        flush();
        fs.flush();
    }

    public void flush() {
        if (dataBlock <= 0) dataBlock = fs.allocateBlock();
        if (dataBlock <= 0) throw new FileSystemIoException("Data block outside of filesystem bounds");

        this.disk.isWithinSpace(dataBlock);
        this.disk.isWithinSpace(dataBlock);

        this.children.forEach((key, value) -> value.flush());

        buffer.clear();
        buffer.position(0);
        buffer.put(name.getBytes());
        buffer.position(16);
        buffer.putLong(length);
        buffer.putLong(dataBlock);
        buffer.putLong(created);
        buffer.putLong(lastAccessed);
        buffer.putLong(lastModified);
        buffer.putInt(0);
        buffer.flip();
        disk.writeBlock((int) Math.floorDiv(address, BLOCK_SIZE), buffer, 0, buffer.capacity());

        buffer.clear();
        buffer.position(0);
        buffer.putInt(children.size());
        for (FSNode child : children.values()) {
            if (child.getAddress() == -1) {
                child.flush();
            }
            buffer.putLong(child.getAddress());
            buffer.putInt(child.isDirectory() ? 0 : 1);
        }
        buffer.flip();

        if ((int) dataBlock <= 0)
            throw new HardError(new FileSystemIoException("FS mutation detection: " + (int) dataBlock));
        disk.writeBlock((int) dataBlock, buffer, 0, buffer.capacity());
        fs.flush();
    }

    void rename(String name, FSFile fsFile) {
        this.children.remove(this.name);
        this.children.put(name, fsFile);
        fsFile.name = name;
    }

    public FSNode[] list() {
        return children.values().toArray(new FSNode[0]);
    }
}
