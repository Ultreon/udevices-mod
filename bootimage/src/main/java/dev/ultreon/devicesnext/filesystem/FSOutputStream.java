package dev.ultreon.devicesnext.filesystem;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class FSOutputStream extends OutputStream {
    private final FSFile node;
    private final SeekableByteChannel channel;

    public FSOutputStream(CraftFS.FSByteChannel channel) {
        this.node = channel.getFile();
        this.channel = channel;
    }

    @Override
    public void write(int b) throws IOException {
        channel.write(ByteBuffer.wrap(new byte[]{(byte) b}));
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        channel.write(ByteBuffer.wrap(b));
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        channel.write(ByteBuffer.wrap(b, off, len));
    }

    @Override
    public void flush() throws IOException {
        node.flush();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
