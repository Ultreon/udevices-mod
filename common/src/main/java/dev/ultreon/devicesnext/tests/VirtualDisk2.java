package dev.ultreon.devicesnext.tests;

import java.io.*;
import java.time.Instant;
import java.util.*;

public class VirtualDisk2 {
    private static final int BLOCK_SIZE = 512; // Size of a block in bytes
    private static final int TOTAL_BLOCKS = 2048; // Number of blocks in the disk
    private static final int FILE_TABLE_BLOCKS = 4; // Blocks reserved for the file table
    private static final String DISK_FILE = "virtual_disk.bin";

    private RandomAccessFile disk; // The virtual disk file
    private DirectoryEntry rootDirectory; // Root directory of the file system

    public VirtualDisk2() throws IOException {
        disk = new RandomAccessFile(DISK_FILE, "rw");
        disk.setLength(TOTAL_BLOCKS * BLOCK_SIZE); // Initialize disk size
        rootDirectory = new DirectoryEntry("/");
        loadFileTable();
    }

    // Create a file
    public boolean createFile(String path, byte[] data) throws IOException {
        String[] parts = splitPath(path);
        DirectoryEntry parent = navigateToParent(parts[0]);
        String fileName = parts[1];

        if (parent == null || parent.containsEntry(fileName)) {
            System.out.println("File already exists or invalid path!");
            return false;
        }

        int requiredBlocks = (int) Math.ceil((double) data.length / BLOCK_SIZE);
        List<Integer> freeBlocks = findFreeBlocks(requiredBlocks);

        if (freeBlocks.size() < requiredBlocks) {
            System.out.println("Not enough space!");
            return false;
        }

        // Write the file data to the allocated blocks
        int offset = 0;
        for (int block : freeBlocks) {
            disk.seek(block * BLOCK_SIZE);
            int length = Math.min(BLOCK_SIZE, data.length - offset);
            disk.write(data, offset, length);
            offset += length;
        }

        FileEntry fileEntry = new FileEntry(fileName, freeBlocks, data.length);
        parent.addEntry(fileEntry);
        saveFileTable();
        return true;
    }

    // Create a directory
    public boolean createDirectory(String path) throws IOException {
        String[] parts = splitPath(path);
        DirectoryEntry parent = navigateToParent(parts[0]);
        String dirName = parts[1];

        if (parent == null || parent.containsEntry(dirName)) {
            System.out.println("Directory already exists or invalid path!");
            return false;
        }

        DirectoryEntry newDir = new DirectoryEntry(dirName);
        parent.addEntry(newDir);
        saveFileTable();
        return true;
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
            disk.seek(block * BLOCK_SIZE);
            int length = Math.min(BLOCK_SIZE, data.length - offset);
            disk.read(data, offset, length);
            offset += length;
        }

        return data;
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

    // Defragment the disk
    public void defragment() throws IOException {
        System.out.println("Starting defragmentation...");
        int nextFreeBlock = FILE_TABLE_BLOCKS;
        Map<Integer, Integer> blockMapping = new HashMap<>();

        // Move all file blocks to create contiguous regions
        defragmentDirectory(rootDirectory, nextFreeBlock, blockMapping);

        // Zero out unused blocks
        byte[] emptyBlock = new byte[BLOCK_SIZE];
        for (int i = nextFreeBlock; i < TOTAL_BLOCKS; i++) {
            disk.seek(i * BLOCK_SIZE);
            disk.write(emptyBlock);
        }

        saveFileTable();
        System.out.println("Defragmentation completed.");
    }

    private int defragmentDirectory(DirectoryEntry directory, int nextFreeBlock, Map<Integer, Integer> blockMapping) throws IOException {
        for (Entry entry : directory.entries.values()) {
            if (entry instanceof FileEntry fileEntry) {
                List<Integer> newBlocks = new ArrayList<>();
                for (int oldBlock : fileEntry.blocks) {
                    if (!blockMapping.containsKey(oldBlock)) {
                        // Copy data from old block to new block
                        disk.seek(oldBlock * BLOCK_SIZE);
                        byte[] buffer = new byte[BLOCK_SIZE];
                        disk.read(buffer);

                        disk.seek(nextFreeBlock * BLOCK_SIZE);
                        disk.write(buffer);

                        blockMapping.put(oldBlock, nextFreeBlock);
                        nextFreeBlock++;
                    }
                    newBlocks.add(blockMapping.get(oldBlock));
                }
                fileEntry.blocks = newBlocks; // Update file's block mapping
            } else if (entry instanceof DirectoryEntry dirEntry) {
                nextFreeBlock = defragmentDirectory(dirEntry, nextFreeBlock, blockMapping);
            }
        }
        return nextFreeBlock;
    }

    // Find free blocks on the disk
    private List<Integer> findFreeBlocks(int count) throws IOException {
        List<Integer> freeBlocks = new ArrayList<>();
        byte[] buffer = new byte[BLOCK_SIZE];
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
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

    // Save the file table to the reserved blocks
    private void saveFileTable() throws IOException {
        disk.seek((TOTAL_BLOCKS - FILE_TABLE_BLOCKS) * BLOCK_SIZE);
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

    // Load the file table from the reserved blocks
    private void loadFileTable() throws IOException {
        disk.seek((TOTAL_BLOCKS - FILE_TABLE_BLOCKS) * BLOCK_SIZE);
        byte[] tableBytes = new byte[FILE_TABLE_BLOCKS * BLOCK_SIZE];
        disk.read(tableBytes);

        ByteArrayInputStream bis = new ByteArrayInputStream(tableBytes);
        DataInputStream dis = new DataInputStream(bis);

        rootDirectory = new DirectoryEntry(dis);
    }

    private void close() throws IOException {
        disk.close();
    }


    // Base entry class
    private abstract static class Entry {
        String name;
        Instant created = Instant.now();
        Instant modified = Instant.now();
        Instant accessed = Instant.now();
        int flags; // Flags for permissions, read-only, etc.

        Entry(String name) {
            this.name = name;
        }

        abstract void serialize(DataOutputStream dos) throws IOException;
    }

    // File entry
    private static class FileEntry extends Entry {
        List<Integer> blocks;
        int size;

        FileEntry(String name, List<Integer> blocks, int size) {
            super(name);
            this.blocks = blocks;
            this.size = size;
        }

        FileEntry(String name, List<Integer> blocks, int size, DataInputStream dis) throws IOException {
            super(name);
            this.blocks = blocks;
            this.size = size;

            deserializeMetadata(dis);
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

        private void deserializeMetadata(DataInputStream dis) throws IOException {
            created = Instant.ofEpochMilli(dis.readLong());
            modified = Instant.ofEpochMilli(dis.readLong());
            accessed = Instant.ofEpochMilli(dis.readLong());
            flags = dis.readInt();
        }

        void updateAccessTime() {
            this.accessed = Instant.now();
        }

        void updateModifiedTime() {
            this.modified = Instant.now();
        }
    }

    // Directory entry
    private static class DirectoryEntry extends Entry {
        Map<String, Entry> entries = new HashMap<>();

        DirectoryEntry(String name) {
            super(name);
        }

        DirectoryEntry(DataInputStream dis) throws IOException {
            super(dis.readUTF());
            dis.readChar();
            int entryCount = dis.readInt();
            if (entryCount > 256) throw new IOException("Entry count overflow " + entryCount + " > 256");
            for (int i = 0; i < entryCount; i++) {
                String entryName = dis.readUTF();
                char type = dis.readChar();
                if (type == 'F') {
                    int size = dis.readInt();
                    int blockCount = dis.readInt();
                    List<Integer> blocks = new ArrayList<>();
                    for (int j = 0; j < blockCount; j++) {
                        blocks.add(dis.readInt());
                    }
                    entries.put(entryName, new FileEntry(entryName, blocks, size, dis));
                } else if (type == 'D') {
                    DirectoryEntry dir = new DirectoryEntry(dis);
                    entries.put(entryName, dir);
                }
            }

            deserializeMetadata(dis);
        }

        void addEntry(Entry entry) {
            entries.put(entry.name, entry);
        }

        Entry getEntry(String name) {
            return entries.get(name);
        }

        boolean containsEntry(String name) {
            return entries.containsKey(name);
        }

        Entry removeEntry(String name) {
            return entries.remove(name);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        List<String> listEntries() {
            return new ArrayList<>(entries.keySet());
        }

        @Override
        void serialize(DataOutputStream dos) throws IOException {
            dos.writeUTF(name);
            dos.writeChar('D');
            dos.writeInt(entries.size());
            for (Entry entry : entries.values()) {
                entry.serialize(dos);
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
    }

    public static void main(String[] args) {
        try {
            VirtualDisk2 disk = new VirtualDisk2();

            byte[] file1 = "Hello, Virtual Disk!".getBytes();
            byte[] file2 = "Another file.".getBytes();

            byte[] file3 = "Yet another file.".getBytes();
            byte[] file4 = "Last file!".getBytes();

            disk.createFile("file1.txt", file1);
            disk.createFile("file2.txt", file2);

            disk.createDirectory("docs");
            disk.createFile("docs/file1.txt", file3);
            disk.createFile("docs/file2.txt", file4);

            System.out.println(new String(disk.readFile("file1.txt"))); // Should print: Hello, Virtual Disk!
            disk.delete("file1.txt");

            System.out.println(new String(disk.readFile("docs/file1.txt"))); // Should print: Hello, Virtual Disk!
            disk.defragment(); // Rearranges the disk to optimize storage
            disk.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
