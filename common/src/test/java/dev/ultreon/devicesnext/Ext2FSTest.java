package dev.ultreon.devicesnext;

import dev.ultreon.devicesnext.fs.Ext2FS;
import org.jnode.fs.FileSystemException;
import org.jnode.partitions.PartitionTableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class Ext2FSTest {
    @Test
    @DisplayName("Read/Write data")
    void test() throws IOException, FileSystemException, PartitionTableException {
        Ext2FS disk = Ext2FS.format(Paths.get("test.vdisk"), 16 * 1024 * 1024);
        disk.createFile(Path.of("/test"), "Hello, Virtual Disk!".getBytes(StandardCharsets.UTF_8));

        Iterator<String> iterator = disk.listDirectory(Path.of("/"));
        while (iterator.hasNext()) {
            String next = iterator.next();
            System.out.println(next);
        }

        assertTrue(disk.exists(Path.of("/test")));
        disk.close();

        Ext2FS disk2 = Ext2FS.open(Paths.get("test.vdisk"));
        try (InputStream data = disk2.read(Path.of("/test"))) {
            byte[] bytes = data.readAllBytes();
            assertEquals("Hello, Virtual Disk!", new String(bytes, StandardCharsets.UTF_8));
        }
    }
}
