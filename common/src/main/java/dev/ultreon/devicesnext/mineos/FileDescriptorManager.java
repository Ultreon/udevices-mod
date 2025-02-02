package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.device.hardware.FSFile;

import java.util.BitSet;

class FileDescriptorManager {
    private static final int MAX_FILE_DESCRIPTORS = 256;

    private final FileDescriptor[] descriptors = new FileDescriptor[MAX_FILE_DESCRIPTORS];
    private final BitSet inUse = new BitSet(MAX_FILE_DESCRIPTORS);
    private final OperatingSystemImpl operatingSystem;

    public FileDescriptorManager(OperatingSystemImpl operatingSystem) {
        this.operatingSystem = operatingSystem;
        for (int i = 0; i < MAX_FILE_DESCRIPTORS; i++) {
            this.descriptors[i] = new FileDescriptor(i, null, null);
        }
    }

    public FileDescriptor allocate() {
        synchronized (this) {
            int index = this.inUse.nextClearBit(0);
            if (index == -1) {
                throw new FileSystemIoException("Out of file descriptors");
            }
            this.inUse.set(index);
            return this.descriptors[index];
        }
    }

    public void free(FileDescriptor descriptor) {
        synchronized (this) {
            int index = descriptor.fd();
            this.inUse.clear(index);

            descriptor.getFile().close();
        }
    }

    public int inUse() {
        synchronized (this) {
            return this.inUse.cardinality();
        }
    }

    public FileDescriptor get(int fd) {
        synchronized (this) {
            return this.descriptors[fd];
        }
    }

    public int open(String path, FSFile fsFile, int flags) {
        synchronized (this) {
            fsFile.open();
            int index = this.inUse.nextClearBit(0);
            if (index == -1) {
                throw new FileSystemIoException("Out of file descriptors");
            }
            this.inUse.set(index);
            this.descriptors[index].open(path, fsFile, flags);
            return index;
        }
    }

    public OperatingSystemImpl getOperatingSystem() {
        return operatingSystem;
    }
}
