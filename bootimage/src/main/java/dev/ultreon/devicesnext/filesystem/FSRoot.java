package dev.ultreon.devicesnext.filesystem;

import static dev.ultreon.devicesnext.filesystem.Disk.BLOCK_SIZE;

public class FSRoot extends FSDirectory {
    public FSRoot(Disk disk, FileSystem fs) {
        super(disk, fs, null, (long) fs.allocateBlock() * BLOCK_SIZE);
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    public boolean isFile() {
        return super.isFile();
    }

    @Override
    public String getName() {
        return "/";
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    public void init() {
        opened = true;
    }
}
