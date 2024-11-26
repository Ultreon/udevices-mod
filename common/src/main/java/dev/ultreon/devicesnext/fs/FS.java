package dev.ultreon.devicesnext.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Iterator;

public interface FS {
    void close() throws IOException;

    InputStream read(Path path, OpenOption... options) throws IOException;

    OutputStream write(Path path, OpenOption... options) throws IOException;

    boolean exists(Path path) throws IOException;

    void flush() throws IOException;

    void createFile(Path path, byte[] data) throws IOException;

    void createDirectory(Path path) throws IOException;

    Iterator<String> listDirectory(Path of) throws IOException;

    void delete(Path path) throws IOException;

    long size(Path path) throws IOException;

    void rename(Path from, String name) throws IOException;
}
