package dev.ultreon.devicesnext.filesystem;

import org.jetbrains.annotations.NotNull;
import org.jnode.fs.FileSystemException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.UUID;

public class BootImager {
    public static void main(String[] args) throws FileSystemException, IOException {
        if (!Files.exists(Path.of("../common/src/main/resources/data/udevices/filesystems"))) {
            Files.createDirectories(Path.of("../common/src/main/resources/data/udevices/filesystems"));
        }

        if (Files.exists(Path.of("../common/src/main/resources/data/udevices/filesystems/main.ext2"))) {
            Files.delete(Path.of("../common/src/main/resources/data/udevices/filesystems/main.ext2"));
        }

        try (FS fs = Ext2FS.format(Path.of("../common/src/main/resources/data/udevices/filesystems/main.ext2"), 16 * 1024 * 1024)) {
            try (var walk = Files.walk(Path.of("src/fs"))) {
                walk.forEach(path -> {
                    try {
                        if (getString(path).equals("src") || getString(path).equals("src/") || getString(path).equals("src/fs") || getString(path).equals("src/fs/"))
                            return;
                        String replace = path.toString().replace("src/fs/", "");
                        if (replace.startsWith(".")) {
                            return;
                        }
                        PathHandle rel = new PathHandle("/" + replace);
                        PathHandle parent = new PathHandle("/");
                        for (String element : rel.getParent()) {
                            parent = parent.child(element);
                            if (!parent.isRoot() && !fs.exists(parent)) {
                                fs.createDirectory(parent);
                            }
                        }
                        if (Files.isDirectory(path)) {
                            fs.createDirectory(rel);
                            return;
                        }
                        byte[] data = Files.readAllBytes(path);
                        try (SeekableByteChannel open = fs.open(rel, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                            open.write(ByteBuffer.wrap(data));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            fs.flush();
        }
    }

    private static @NotNull String getString(Path path) {
        return path.toString().replace("\\", "/");
    }
}