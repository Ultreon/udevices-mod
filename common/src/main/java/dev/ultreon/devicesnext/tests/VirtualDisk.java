package dev.ultreon.devicesnext.tests;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

public class VirtualDisk {
    private static final int BLOCK_SIZE = 512; // Size of a block in bytes
    private static final int TOTAL_BLOCKS = 1000; // Number of blocks in the disk
    private static final int FILE_TABLE_BLOCKS = 4; // Blocks reserved for the file table
    private static final String DISK_FILE = "virtual_disk.bin";
    public static final int DIR_TABLE_SIZE = 2048;

    private RandomAccessFile disk; // The virtual disk file
    private DirectoryEntry rootDirectory; // Root directory of the file system

    public VirtualDisk() throws IOException {
        disk = new RandomAccessFile(DISK_FILE, "rw");
        disk.setLength(TOTAL_BLOCKS * BLOCK_SIZE); // Initialize disk size
        rootDirectory = new DirectoryEntry(this, "/");
        loadFileTable();
    }

    // Create a file
    public void createFile(String path, byte[] data) throws IOException {
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        DirectoryEntry parent = navigateToDirectory(parentPath);

        List<Integer> blocks = allocateBlocks(data.length);
        writeToBlocks(blocks, data);

        FileEntry fileEntry = new FileEntry(fileName, blocks, data.length);
        parent.addEntry(fileEntry);
        saveFileTable();
    }

    private void writeToBlocks(List<Integer> blocks, byte[] data) throws IOException {
        int offset = 0;
        for (int block : blocks) {
            disk.seek(block * BLOCK_SIZE);
            int len = Math.min(BLOCK_SIZE, data.length - offset);
            disk.write(data, offset, len);
            offset += len;
        }
    }

    // Find free blocks on the disk
    private List<Integer> findFreeBlocks(int count) throws IOException {
        List<Integer> freeBlocks = new ArrayList<>();
        byte[] buffer = new byte[BLOCK_SIZE];
        for (int i = DIR_TABLE_SIZE / BLOCK_SIZE; i < TOTAL_BLOCKS; i++) {
            disk.seek(i * BLOCK_SIZE);
            disk.read(buffer);
            if (isBlockFree(buffer)) {
                freeBlocks.add(i);
                if (freeBlocks.size() == count) {
                    break;
                }
            }
        }
        return freeBlocks;
    }

    // Check if a block is free
    private boolean isBlockFree(byte[] block) {
        for (byte b : block) {
            if (b != 0) return false;
        }
        return true;
    }

    private List<Integer> allocateBlocks(int length) throws IOException {
        int requiredBlocks = (int) Math.ceil((double) length / BLOCK_SIZE);
        List<Integer> freeBlocks = findFreeBlocks(requiredBlocks);

        if (freeBlocks.size() < requiredBlocks) {
            throw new IOException("Not enough space!");
        }

        return freeBlocks;
    }

    // Create a directory
    public void createDirectory(String path) throws IOException {
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        String dirName = path.substring(path.lastIndexOf('/') + 1);
        DirectoryEntry parent = navigateToDirectory(parentPath);

        DirectoryEntry newDirectory = new DirectoryEntry(this, dirName);
        parent.addEntry(newDirectory);
        newDirectory.flush();
    }

    // Retrieve metadata
    public void printMetadata(String path) throws IOException {
        Entry entry = navigateToEntry(path);
        if (entry instanceof FileEntry file) {
            System.out.println("File: " + file.name);
            System.out.println("Created: " + file.created);
            System.out.println("Modified: " + file.modified);
            System.out.println("Accessed: " + file.accessed);
            System.out.println("Flags: " + file.flags);
        } else if (entry instanceof DirectoryEntry dir) {
            System.out.println("Directory: " + dir.name);
            System.out.println("Created: " + dir.created);
            System.out.println("Modified: " + dir.modified);
            System.out.println("Accessed: " + dir.accessed);
            System.out.println("Flags: " + dir.flags);
        }
    }

    // Helper to navigate to a directory
    private DirectoryEntry navigateToDirectory(String path) throws IOException {
        Entry entry = navigateToEntry(path);
        if (entry instanceof DirectoryEntry dir) {
            return dir;
        } else {
            throw new IOException("Path is not a directory: " + path);
        }
    }

    // Navigate to any entry (file or directory)
    private Entry navigateToEntry(String path) throws IOException {
        String[] parts = path.split("/");
        Entry current = rootDirectory;
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (current instanceof DirectoryEntry dir) {
                current = dir.entries.get(part);
                if (current == null) throw new IOException("Path not found: " + path);
            } else {
                throw new IOException("Invalid path: " + path);
            }
        }
        return current;
    }

    private int[] getFileOffset(String path) {
        String[] parts = splitPath(path);
        DirectoryEntry parent = navigateToParent(parts[0]);
        String fileName = parts[1];

        if (parent == null || !parent.containsEntry(fileName)) {
            System.out.println("File not found!");
            return null;
        }

        FileEntry fileEntry = (FileEntry) parent.getEntry(fileName);
        IntStream intStream = fileEntry.blocks.stream().mapToInt(Integer::intValue).map(i -> i * BLOCK_SIZE);
        return intStream.toArray();
    }

    // Read a file
    public byte[] readFile(String path) throws IOException {
        String[] parts = splitPath(path);
        DirectoryEntry parent = navigateToParent(parts[0]);
        String fileName = parts[1];

        if (parent == null || !parent.containsEntry(fileName)) {
            System.out.println("File not found!");
            return null;
        }

        FileEntry fileEntry = (FileEntry) parent.getEntry(fileName);
        byte[] data = new byte[fileEntry.size];
        int offset = 0;
        for (int block : fileEntry.blocks) {
            int length = readBlock(block, data, offset);
            offset += length;
        }

        return data;
    }

    private int readBlock(int block, byte[] data, int offset) throws IOException {
        disk.seek((long) block * BLOCK_SIZE);
        int length = Math.min(BLOCK_SIZE, data.length - offset);
        disk.read(data, offset, length);
        return length;
    }
    // Delete a file or directory

    public boolean delete(String path) throws IOException {
        String[] parts = splitPath(path);
        DirectoryEntry parent = navigateToParent(parts[0]);
        String name = parts[1];

        if (parent == null || !parent.containsEntry(name)) {
            System.out.println("File or directory not found!");
            return false;
        }

        Entry entry = parent.removeEntry(name);

        if (entry instanceof FileEntry fileEntry) {
            // Clear the blocks for files
            for (int block : fileEntry.blocks) {
                disk.seek(block * BLOCK_SIZE);
                disk.write(new byte[BLOCK_SIZE]);
            }
        } else if (entry instanceof DirectoryEntry dirEntry) {
            if (!dirEntry.isEmpty()) {
                System.out.println("Cannot delete non-empty directory!");
                parent.addEntry(dirEntry); // Restore entry
                return false;
            }
        }

        saveFileTable();
        return true;
    }
    // List entries in a directory

    public List<String> listDirectory(String path) {
        DirectoryEntry dir = navigateToParent(path);
        if (dir == null) {
            System.out.println("Directory not found!");
            return Collections.emptyList();
        }
        return dir.listEntries();
    }
    // Split the path into directory and name

    private String[] splitPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash == -1) {
            return new String[]{"/", path};
        }
        String parentPath = path.substring(0, lastSlash);
        String name = path.substring(lastSlash + 1);
        return new String[]{parentPath.isEmpty() ? "/" : parentPath, name};
    }
    // Navigate to the parent directory

    private DirectoryEntry navigateToParent(String path) {
        String[] parts = path.split("/");
        DirectoryEntry current = rootDirectory;
        for (String part : parts) {
            if (part.isEmpty()) continue;
            Entry entry = current.getEntry(part);
            if (!(entry instanceof DirectoryEntry)) {
                return null; // Invalid path
            }
            current = (DirectoryEntry) entry;
        }
        return current;
    }
    // Metadata-enhanced entry classes
    private abstract static class Entry {
        String name;
        Instant created;
        Instant modified;
        Instant accessed;

        int flags; // Flags for permissions, read-only, etc.

        Entry(String name) {
            this.name = name;
            Instant now = Instant.now();
            this.created = now;
            this.modified = now;
            this.accessed = now;
            this.flags = 0;
        }

        abstract void serialize(DataOutputStream dos) throws IOException;

        void updateAccessTime() {
            this.accessed = Instant.now();
        }
        void updateModifiedTime() {
            this.modified = Instant.now();
        }

    }
    private static class FileEntry extends Entry {
        List<Integer> blocks;

        int size;

        FileEntry(String name, List<Integer> blocks, int size) {
            super(name);
            this.blocks = blocks;
            this.size = size;
        }

        @Override
        void serialize(DataOutputStream dos) throws IOException {
            dos.writeUTF(name);
            dos.writeChar('F');
            dos.writeInt(size);
            dos.writeInt(blocks.size());
            for (int block : blocks) {
                dos.writeInt(block);
            }
            serializeMetadata(dos);
        }
        private void serializeMetadata(DataOutputStream dos) throws IOException {
            dos.writeLong(created.toEpochMilli());
            dos.writeLong(modified.toEpochMilli());
            dos.writeLong(accessed.toEpochMilli());
            dos.writeInt(flags);
        }

    }
    // Save the file table

    private void saveFileTable() throws IOException {
        disk.seek(0);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        rootDirectory.serialize(dos);

        byte[] tableBytes = bos.toByteArray();
        if (tableBytes.length > FILE_TABLE_BLOCKS * BLOCK_SIZE) {
            throw new IOException("File table exceeds reserved space!");
        }

        disk.write(tableBytes);
        disk.write(new byte[FILE_TABLE_BLOCKS * BLOCK_SIZE - tableBytes.length]); // Fill remaining space with zeros
    }
    // Load the file table

    private void loadFileTable() throws IOException {
        disk.seek(0);
        byte[] tableBytes = new byte[FILE_TABLE_BLOCKS * BLOCK_SIZE];
        disk.read(tableBytes);

        ByteArrayInputStream bis = new ByteArrayInputStream(tableBytes);
        DataInputStream dis = new DataInputStream(bis);

        rootDirectory = new DirectoryEntry(this, dis);
    }
    private static class DirectoryEntry extends Entry {
        Map<String, Entry> entries = new HashMap<>();
        private final VirtualDisk vd;

        private List<Integer> blocks = new ArrayList<>();

        DirectoryEntry(VirtualDisk vd, String name) {
            super(name);
            this.vd = vd;
        }

        DirectoryEntry(VirtualDisk vd, DataInputStream dis) throws IOException {
            super(dis.readUTF());
            dis.readChar();
            this.vd = vd;
            blocks.clear();
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                blocks.add(dis.readInt());
            }

            deserializeMetadata(dis);

            loadTable();
        }

        private void loadTable() throws IOException {
            byte[] data = new byte[DIR_TABLE_SIZE];
            int offset = 0;
            for (int block : blocks) {
                int length = vd.readBlock(block, data, offset);
                offset += length;
            }
        }

        void deserializeTable(DataInputStream dis) throws IOException {
            int blockCount = dis.readInt();
            blocks = new ArrayList<>(blockCount);
            for (int i = 0; i < blockCount; i++) {
                blocks.add(dis.readInt());
            }
            deserializeMetadata(dis);
        }

        void addEntry(Entry entry) throws IOException {
            entries.put(entry.name, entry);
            updateModifiedTime();
            flush();
        }

        @Override
        void serialize(DataOutputStream dos) throws IOException {
            dos.writeUTF(name);
            dos.writeChar('D');
            dos.writeInt(blocks.size());
            for (int block : blocks) {
                dos.writeInt(block);
            }
            serializeMetadata(dos);
        }

        private void serializeMetadata(DataOutputStream dos) throws IOException {
            dos.writeLong(created.toEpochMilli());
            dos.writeLong(modified.toEpochMilli());
            dos.writeLong(accessed.toEpochMilli());
            dos.writeInt(flags);
        }

        private void deserializeMetadata(DataInputStream dis) throws IOException {
            created = Instant.ofEpochMilli(dis.readLong());
            modified = Instant.ofEpochMilli(dis.readLong());
            accessed = Instant.ofEpochMilli(dis.readLong());
            flags = dis.readInt();
        }

        public void flush() throws IOException {
            List<Integer> integers = vd.allocateBlocks(DIR_TABLE_SIZE);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(entries.size());
            for (Entry entry : entries.values()) {
                entry.serialize(dos);
            }
            this.blocks = integers;
            System.out.println("Writing to: " + blocks.stream().map(i -> String.format("%08x", i * BLOCK_SIZE)).toList());
            vd.writeToBlocks(integers, bos.toByteArray());
        }

        Entry getEntry(String name) {
            return entries.get(name);
        }

        boolean containsEntry(String name) {
            return entries.containsKey(name);
        }

        Entry removeEntry(String name) throws IOException {
            Entry remove = entries.remove(name);
            flush();
            return remove;
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }
        public List<String> listEntries() {
            return List.copyOf(entries.keySet());
        }

    }

    public static void main(String[] args) {
        try {
            VirtualDisk disk = new VirtualDisk();

            // Create directories and files
            disk.createDirectory("/docs");
            disk.createFile("/docs/file1.txt", "Hello, World!".getBytes());

            // Print metadata
            disk.printMetadata("/docs");
            disk.printMetadata("/docs/file1.txt");

            byte[] bytes = disk.readFile("/docs/file1.txt");
            int[] fileOffset = disk.getFileOffset("/docs/file1.txt");
            System.out.println("fileOffset = " + Arrays.toString(Arrays.stream(fileOffset).mapToObj("%08x"::formatted).toArray()));
            String s = new String(bytes);
            System.out.println("s = " + s);

            disk = new VirtualDisk();
            // Print metadata
            disk.printMetadata("/docs");
            disk.printMetadata("/docs/file1.txt");

            bytes = disk.readFile("/docs/file1.txt");
            fileOffset = disk.getFileOffset("/docs/file1.txt");
            System.out.println("fileOffset = " + Arrays.toString(Arrays.stream(fileOffset).mapToObj("%08x"::formatted).toArray()));
            s = new String(bytes);
            System.out.println("s = " + s);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
