package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.mineos.Disk;
import dev.ultreon.devicesnext.mineos.FileSystem;
import dev.ultreon.devicesnext.mineos.FileSystemIoException;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.WeakHashMap;

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
    private final Map<String, FSNode> children = new WeakHashMap<>();

    protected final FileSystem fs;
    boolean opened = false;

    public FSDirectory(Disk diskIn, FileSystem fsIn, FSDirectory parentIn, long addressIn) {
        disk = diskIn;
        parent = parentIn;
        address = addressIn;
        fs = fsIn;

        if (addressIn < BLOCK_SIZE) {
            throw new IllegalArgumentException("Invalid address: " + addressIn);
        }

        diskIn.readBlock(Math.floorDiv(addressIn, BLOCK_SIZE), buffer);
        buffer.flip();
        buffer.position(0);

        byte b = buffer.get();
        byte[] dst = new byte[16];
        buffer.get(1, dst);
        if (b > dst.length) {
            UDevicesMod.LOGGER.warn("File name too long: " + b + " > " + dst.length);
            dst = new byte[b];
        }
        name = new String(dst, 0, b);
        buffer.position(16);
        length = buffer.getLong();
        dataBlock = buffer.getLong();
        diskIn.isWithinSpace(dataBlock);

        created = buffer.getLong();
        lastAccessed = buffer.getLong();
        lastModified = buffer.getLong();

        buffer.clear();
    }

    FSDirectory(Disk diskIn, FileSystem fsIn, FSDirectory parentIn) {
        disk = diskIn;
        fs = fsIn;
        parent = parentIn;
        address = (long) fsIn.allocateBlock() * BLOCK_SIZE;
        dataBlock = fsIn.allocateBlock();
        opened = true;
    }

    @Override
    public void open() {
        if (opened && !(this instanceof FSRoot)) return;

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

            opened = true;
        } else {
            children.clear();
            flush();
        }
    }

    @Override
    public void close() {
        opened = false;
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
        if (!opened) throw new IllegalStateException("Directory node not opened!");
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
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    public boolean isRoot() {
        return false;
    }

    public void createFile(String name) {
        if (!opened) throw new IllegalStateException("Directory node not opened!");
        if (this.children.containsKey(name)) throw new FileSystemIoException("File already exists");

        FSFile file = new FSFile(disk, fs, this);
        file.name = name;
        file.setCreated(System.nanoTime());
        file.setLastAccessed(System.nanoTime());
        file.setLastModified(System.nanoTime());
        file.setLength(0);
        file.setMode(0);
        file.flush();
        file.close();
        this.children.put(name, file);

        flush();
        fs.flush();
    }

    public void createDirectory(String name) {
        if (!opened) throw new IllegalStateException("Directory node not opened!");
        if (this.children.containsKey(name)) throw new FileSystemIoException("File already exists");

        FSDirectory value = new FSDirectory(disk, fs, this);
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
        if (!opened) throw new IllegalStateException("Directory node not opened!");
        if (dataBlock <= 0) dataBlock = fs.allocateBlock();
        if (dataBlock <= 0) throw new FileSystemIoException("Data block outside of filesystem bounds");

        this.disk.isWithinSpace(dataBlock);
        this.disk.isWithinSpace(dataBlock);

        this.children.forEach((key, value) -> {
            if (value.isOpen()) {
                value.flush();
            }
        });

        if (name.length() >= 16) {
            throw new IllegalArgumentException("Name too long: " + name);
        }

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

    @Override
    public boolean isOpen() {
        return opened;
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
