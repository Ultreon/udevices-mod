package dev.ultreon.devicesnext.mineos;

import java.io.IOException;

public class FileSystemIoException extends RuntimeException {
    @org.jetbrains.annotations.NotNull
    private final IOException e;

    public FileSystemIoException(IOException e) {
        super(e.getMessage());
        this.e = e;
    }

    public FileSystemIoException(String failedToReadBlock, IOException e) {
        super(failedToReadBlock);
        this.e = e;
    }

    public FileSystemIoException(String outOfFileDescriptors) {
        super(outOfFileDescriptors);
        this.e = new IOException(outOfFileDescriptors);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return this.e.getStackTrace();
    }

    @Override
    public Throwable getCause() {
        return this.e.getCause();
    }

    @Override
    public void printStackTrace() {
        this.e.printStackTrace();
    }

    @Override
    public void printStackTrace(java.io.PrintStream s) {
        this.e.printStackTrace(s);
    }

    @Override
    public void printStackTrace(java.io.PrintWriter s) {
        this.e.printStackTrace(s);
    }

    @Override
    public String toString() {
        return this.e.toString();
    }

    @Override
    public int hashCode() {
        return this.e.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.e.equals(obj);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this.e.fillInStackTrace();
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        return this.e.initCause(cause);
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.e.setStackTrace(stackTrace);
    }
}
