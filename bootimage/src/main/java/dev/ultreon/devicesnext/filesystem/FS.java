package dev.ultreon.devicesnext.filesystem;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.util.Iterator;

public interface FS extends AutoCloseable {
    void close() throws IOException;

    SeekableByteChannel open(PathHandle path, OpenOption... options) throws IOException;

    boolean exists(PathHandle path) throws IOException;

    void flush() throws IOException;

    void createFile(PathHandle path, byte[] data) throws IOException;

    void createDirectory(PathHandle path) throws IOException;

    Iterator<String> listDirectory(PathHandle of) throws IOException;

    void delete(PathHandle path) throws IOException;

    long size(PathHandle path) throws IOException;

    void rename(PathHandle from, String name) throws IOException;

    void atomicMove(PathHandle from, PathHandle to) throws IOException;

    FileInfo info(PathHandle path) throws IOException;

    void copy(PathHandle virtualPath, PathHandle virtualPath1, boolean overwrite) throws IOException;
}
