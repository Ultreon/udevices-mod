package dev.ultreon.devicesnext.filesystem;

public record FileInfo(
        long size,
        boolean isDirectory,
        boolean isFile,
        int mode,
        long mtime,
        long ctime,
        long atime,
        int uid,
        int gid,
        long dev,
        long ino,
        int nlink,
        long rdev,
        int blksize,
        long blocks,
        String name
) {
}
