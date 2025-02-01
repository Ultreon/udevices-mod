package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.mineos.Disk;
import dev.ultreon.devicesnext.mineos.FileSystem;
import dev.ultreon.devicesnext.mineos.FileSystemIoException;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FSDirectory implements FSNode {
    private final Disk disk;
    private final FSDirectory parent;
    private final long address;
    private final ByteBuffer buffer = ByteBuffer.allocate(Disk.BLOCK_SIZE);
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

        disk.readBlock(Math.floorDiv(address, Disk.BLOCK_SIZE), buffer);
        buffer.flip();

        byte[] dst = new byte[16];
        buffer.get(0, dst);
        this.name = new String(dst);

        int idx = 16;
        this.length = buffer.getLong(idx += Long.BYTES);
        this.dataBlock = buffer.getLong(idx += Long.BYTES);
        this.created = buffer.getLong(idx += Long.BYTES);
        this.lastAccessed = buffer.getLong(idx += Long.BYTES);
        this.lastModified = buffer.getLong(idx += Long.BYTES);

        buffer.clear();
    }

    @Override
    public void open() {
        if (!children.isEmpty()) throw new FileSystemIoException("File in use");

        if (dataBlock != -1) {
            disk.readBlock(dataBlock, buffer);
            buffer.flip();
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
        this.children.put(name, new FSFile(disk, fs, this, -1));
    }

    public void createDirectory(String name) {
        this.children.put(name, new FSDirectory(disk, fs, this, -1));
    }

    public void flush() {
        buffer.clear();
        buffer.putInt(children.size());
        for (FSNode child : children.values()) {
            if (child.getAddress() == -1) {
                child.flush();
            }
            buffer.putLong(child.getAddress());
            buffer.putInt(child.isDirectory() ? 0 : 1);
        }
        buffer.flip();
        disk.writeBlock((int) Math.floorDiv(address, Disk.BLOCK_SIZE), buffer, 0, buffer.capacity());
    }

    void rename(String name, FSFile fsFile) {
        this.children.remove(this.name);
        this.children.put(name, fsFile);
        fsFile.name = name;
    }
}
