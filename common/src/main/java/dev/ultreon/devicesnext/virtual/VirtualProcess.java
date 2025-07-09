package dev.ultreon.devicesnext.virtual;

import com.google.common.base.Preconditions;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.mineos.VirtualComputer;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.IntConsumer;

public class VirtualProcess implements Runnable {
    private static final ThreadLocal<CaptureSlot> captureSlot = new ThreadLocal<>();
    private final int pid;
    private final VirtualFileSystem fs;
    private final Value modules;
    private final String init;
    private final String path;
    private final String[] args;
    private final Map<String, String> env;
    private final Process process;
    private IntConsumer onExit;
    private Context processContext;
    private final VirtualProcessApi api = VirtualProcessApi.of(this);
    private Context parent;
    private final Thread thread;
    private final OutputStream stdin = NullOutputStream.INSTANCE;
    private final InputStream stdout = NullInputStream.INSTANCE;
    private final InputStream stderr = NullInputStream.INSTANCE;
    private int exitValue;

    public VirtualProcess(VirtualComputer virtualComputer,
                          int pid,
                          Value modules,
                          String init,
                          String path,
                          String[] args,
                          Map<String, String> env) {
        this.fs = virtualComputer.getFS();
        this.pid = pid;
        this.modules = modules;
        this.init = init;
        this.path = path;
        this.args = args;
        this.env = env;

        this.thread = new Thread(this, "Process: " + path);
        this.process = new Process() {

            @Override
            public OutputStream getOutputStream() {
                return stdin;
            }

            @Override
            public InputStream getInputStream() {
                return stdout;
            }

            @Override
            public InputStream getErrorStream() {
                return stderr;
            }

            @Override
            public int waitFor() throws InterruptedException {
                return waitFor();
            }

            @Override
            public int exitValue() {
                return exitValue;
            }

            @Override
            public void destroy() {
                kill(-1);
            }
        };
    }

    Process getProcess() {
        return process;
    }

    public static Process capture(Callable<Void> o) throws Exception {
        CaptureSlot value = new CaptureSlot();
        captureSlot.set(value);
        o.call();
        return value.process != null ? value.process.process : null;
    }

    public void start() {
        parent = Context.getCurrent();
        VirtualProcessManagement.add(parent, this);

        thread.start();
        CaptureSlot captureSlot1 = captureSlot.get();
        if (captureSlot1 != null) {
            captureSlot1.process = this;
        }
    }

    public void setOnExit(IntConsumer handler) {
        this.onExit = handler;
    }

    public int getPid() {
        return pid;
    }

    public void kill(int code) {
        this.thread.interrupt();
        this.processContext.close(true);
        try {
            this.thread.join();
            onExit(processContext, code);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void onExit(Context self, int code) {
        VirtualProcessManagement.shutdown(self);
        VirtualProcessManagement.remove(parent, this);
        if (onExit != null) onExit.accept(code);
        this.exitValue = code;
        this.notify();
    }

    @Override
    public void run() {
    }

    public VirtualProcessApi api() {
        return api;
    }

    public OutputStream getOutputStream() {
        return stdin;
    }

    public InputStream getInputStream() {
        return stdout;
    }

    public InputStream getErrorStream() {
        return stderr;
    }

    public int waitFor() throws InterruptedException {
        this.wait();
        return exitValue;
    }

    public int exitValue() {
        return exitValue;
    }

    public void destroy() {
        kill(1);
    }

    private static class CaptureSlot {
        private @Nullable VirtualProcess process;

        public void set(@NotNull VirtualProcess process) {
            Preconditions.checkNotNull(process, "process");
            this.process = process;
        }

        public @Nullable VirtualProcess get() {
            return process;
        }
    }
}
