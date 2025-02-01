package dev.ultreon.devicesnext.device.hardware;

import dev.ultreon.devicesnext.mineos.Disk;
import dev.ultreon.devicesnext.mineos.FileSystem;

public class FSRoot extends FSDirectory {
    public FSRoot(Disk disk, FileSystem fs) {
        super(disk, fs, null, 0L);
    }

    @Override
    public boolean isFile() {
        return super.isFile();
    }

    @Override
    public void close() {
        super.close();

        fs.close();
    }

    @Override
    public String getName() {
        return "/";
    }

    @Override
    public boolean isRoot() {
        return true;
    }
}
