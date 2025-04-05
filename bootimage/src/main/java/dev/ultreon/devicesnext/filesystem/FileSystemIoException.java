package dev.ultreon.devicesnext.filesystem;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class FileSystemIoException extends RuntimeException {
    @Nullable
    private final IOException e;

    public FileSystemIoException(IOException e) {
        super(e.getMessage());
        this.e = e;
    }

    public FileSystemIoException(String failedToReadBlock, @Nullable IOException e) {
        super(failedToReadBlock);
        this.e = e;
    }

    public FileSystemIoException(String message) {
        super(message);
        this.e = new IOException(message);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        if (this.e == null) return super.getStackTrace();
        return this.e.getStackTrace();
    }

    @Override
    public Throwable getCause() {
        if (this.e == null) return super.getCause();
        return this.e.getCause();
    }

    @Override
    public void printStackTrace() {
        if (this.e == null) {
            super.printStackTrace();
            return;
        }
        this.e.printStackTrace();
    }

    @Override
    public void printStackTrace(java.io.PrintStream s) {
        if (this.e == null) {
            super.printStackTrace(s);
            return;
        }
        this.e.printStackTrace(s);
    }

    @Override
    public void printStackTrace(java.io.PrintWriter s) {
        if (this.e == null) {
            super.printStackTrace(s);
            return;
        }
        this.e.printStackTrace(s);
    }

    @Override
    public String toString() {
        if (this.e == null) return super.toString();
        return this.e.toString();
    }

    @Override
    public int hashCode() {
        if (this.e == null) return super.hashCode();
        return this.e.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.e == null) return super.equals(obj);
        return this.e.equals(obj);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        if (this.e == null) return super.fillInStackTrace();
        return this.e.fillInStackTrace();
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        if (this.e == null) return super.initCause(cause);
        return this.e.initCause(cause);
    }

    @Override
    public void setStackTrace(StackTraceElement[] stackTrace) {
        if (this.e == null) {
            super.setStackTrace(stackTrace);
            return;
        }
        this.e.setStackTrace(stackTrace);
    }
}
