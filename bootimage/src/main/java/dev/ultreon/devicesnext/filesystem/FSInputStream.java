package dev.ultreon.devicesnext.filesystem;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class FSInputStream extends InputStream {
    private final SeekableByteChannel channel;

    public FSInputStream(CraftFS.FSByteChannel channel) {
        this.channel = channel;
    }

    @Override
    public int read() throws IOException {
        ByteBuffer allocate = ByteBuffer.allocate(1);
        int read = channel.read(allocate);
        if (read == -1) return -1;
        return allocate.get(0) & 0xFF;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        ByteBuffer allocate = ByteBuffer.allocate(len);
        int read = channel.read(allocate);
        if (read == -1) return -1;
        allocate.get(b, off, read);
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long oldPos = channel.position();
        channel.position(oldPos + n);
        return channel.position() - oldPos;
    }

    @Override
    public int available() throws IOException {
        return (int) (channel.size() - channel.position());
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        channel.position(channel.position() + n);
    }

    @Override
    public byte @NotNull [] readAllBytes() throws IOException {
        long size = channel.size();
        if (size > Integer.MAX_VALUE) throw new IOException("File too large");
        byte[] bytes = new byte[(int) size];
        channel.read(ByteBuffer.wrap(bytes));
        return bytes;
    }

    @Override
    public byte @NotNull [] readNBytes(int len) throws IOException {
        byte[] bytes = new byte[len];
        channel.read(ByteBuffer.wrap(bytes));
        return bytes;
    }

    @Override
    public int read(byte @NotNull [] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return read(b, off, len);
    }

    @Override
    public boolean markSupported() {
        return super.markSupported();
    }
}
