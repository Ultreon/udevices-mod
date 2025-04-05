package dev.ultreon.devicesnext.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Disk {
    public static final int BLOCK_SIZE = 512;

    private final UUID owner;
    private final UUID serial;
    private boolean opened;
    private RandomAccessFile io;

    public Disk(UUID owner, UUID serial) {
        this.owner = owner;
        this.serial = serial;
    }

    public void open() {
        synchronized (this) {
            if (this.opened) {
                throw new FileSystemIoException("Disk is already opened");
            }

            try {
                this.io = new RandomAccessFile(owner + "/" + serial + ".img", "rw");
            } catch (FileNotFoundException e) {
                try {
                    File file = new File(owner + "/" + serial + ".img");
                    if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                        throw new FileSystemIoException("Failed to initialize disk");
                    }
                    boolean newFile = file.createNewFile();
                    if (!newFile) {
                        throw new FileSystemIoException("Failed to initialize disk");
                    }

                    this.io = new RandomAccessFile(owner + "/" + serial + ".img", "rw");
                    this.io.setLength(16 * 1024 * 1024);
                    this.io.seek(16 * 1024 * 1024 - 1);
                    this.io.write(0);
                    this.io.seek(0);
                    this.io.getFD().sync();
                } catch (IOException ex) {
                    throw new FileSystemIoException("Failed to initialize disk", ex);
                }
            }
            this.opened = true;
        }
    }

    public void close() {
        synchronized (this) {
            if (!this.opened) {
                throw new FileSystemIoException("Disk is already closed");
            }

            try {
                this.io.close();
            } catch (IOException e) {
                throw new FileSystemIoException("Failed to close disk", e);
            }
            this.io = null;
            this.opened = false;
        }
    }

    public UUID getOwner() {
        return this.owner;
    }

    public UUID getSerial() {
        return this.serial;
    }

    public boolean isOpened() {
        return this.opened;
    }

    public void readBlock(long block, ByteBuffer buffer) {
        synchronized (this) {
            if (!this.opened) {
                throw new FileSystemIoException("Disk is not opened");
            }

            if (block < 0) throw new FileSystemIoException("Invalid block: " + block);

            try {
                long pos = block * BLOCK_SIZE;
                long endPos = pos + buffer.capacity();
                if (endPos > this.length()) throw new FileSystemIoException("Attempted to read beyond disk space: " + endPos + " > " + this.length());
                this.io.seek(pos);
                byte[] buf = new byte[BLOCK_SIZE];
                buffer.limit(BLOCK_SIZE);
                buffer.position(0);
                this.io.read(buf);
                buffer.put(buf);
            } catch (IOException e) {
                throw new FileSystemIoException("Failed to read block", e);
            }
        }
    }

    public void readBlock(int block, ByteBuffer buffer, int offset, int length) {
        synchronized (this) {
            if (!this.opened) {
                throw new FileSystemIoException("Disk is not opened");
            }

            if (block < 0) throw new FileSystemIoException("Invalid block: " + block);
            if (offset < 0) throw new FileSystemIoException("Invalid offset: " + offset);
            if (length < 0) throw new FileSystemIoException("Invalid length: " + length);
            if (offset + length > BLOCK_SIZE) throw new FileSystemIoException("Offset " + (offset + length) + " exceeds block size " + BLOCK_SIZE);

            try {
                this.io.seek((long) block * BLOCK_SIZE + offset);
                byte[] buf = new byte[length];
                this.io.read(buf);
                buffer.put(buf);
            } catch (IOException e) {
                throw new FileSystemIoException("Failed to read block", e);
            }
        }
    }

    public void writeBlock(int block, ByteBuffer buffer, int offset, int length) {
        synchronized (this) {
            if (!this.opened) {
                throw new FileSystemIoException("Disk is not opened");
            }

            if (block < 0)
                throw new FileSystemIoException("Invalid block: " + block);
            if (offset < 0)
                throw new FileSystemIoException("Invalid offset: " + offset);
            if (length < 0)
                throw new FileSystemIoException("Invalid length: " + length);
            if (offset + length > BLOCK_SIZE)
                throw new FileSystemIoException("Offset " + (offset + length) + " exceeds block size " + BLOCK_SIZE);

            try {
                long start = (long) block * BLOCK_SIZE + offset;
                if (start + length > length())
                    throw new FileSystemIoException("Attempted to write beyond disk space.");
                this.io.seek((long) block * BLOCK_SIZE + offset);
                buffer.limit(length);
                buffer.position(0);
                byte[] buf = new byte[length];
                buffer.get(buf, 0, buf.length);
                this.io.write(buf);
            } catch (IOException e) {
                throw new FileSystemIoException(e);
            }
        }
    }

    public long length() {
        synchronized (this) {
            if (!this.opened) {
                throw new FileSystemIoException("Disk is not opened");
            }
            try {
                return this.io.length();
            } catch (IOException e) {
                throw new FileSystemIoException("Failed to get disk length", e);
            }
        }
    }

    public void flush() {
        synchronized (this) {
            if (!this.opened) {
                throw new FileSystemIoException("Disk is not opened");
            }
            try {
                this.io.getFD().sync();
            } catch (IOException e) {
                throw new FileSystemIoException("Failed to flush disk", e);
            }
        }
    }

    public void isWithinSpace(long dataBlock) {
        if (dataBlock * BLOCK_SIZE > length())
            throw new IllegalArgumentException("Impossible data block: " + dataBlock + " @" + (dataBlock * BLOCK_SIZE) + " is beyond disk space: " + length());
    }
}
