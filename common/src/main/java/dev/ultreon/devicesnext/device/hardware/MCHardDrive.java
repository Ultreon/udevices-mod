package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.fs.Ext2FS;
import dev.ultreon.devicesnext.device.McDevice;
import org.jnode.fs.FileSystemException;

import java.io.IOException;
import java.util.UUID;

public class MCHardDrive extends Drive {
    private boolean uninitialized = false;
    private Ext2FS fs;

    public MCHardDrive(McDevice device, UUID id) throws IOException {
        super(device, id, Class.MEDIUM);

        try {
            this.fs = Ext2FS.open(this);
        } catch (IOException | FileSystemException e) {
            this.uninitialized = true;
        }
    }

    @Override
    public Ext2FS getFileSystem() {
        return fs;
    }

    @Override
    public void format() throws IOException {
        try {
            if (fs != null) fs.close();
            this.fs = Ext2FS.format(this, type.bytes);
            this.uninitialized = false;
        } catch (FileSystemException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isUninitialized() {
        return uninitialized;
    }

    @Override
    public UUID getUuid() {
        return null;
    }
}
