package dev.ultreon.devicesnext.device.hardware;

public interface FSNode {
    void open();
    void close();

    String getName();
    FSDirectory getParent();
    FSNode getChild(String name);

    boolean isDirectory();

    default boolean isFile() {
        return !isDirectory();
    }

    long getLastModified();
    long getLastAccessed();
    long getCreated();

    void setLastAccessed(long lastAccessed);
    void setLastModified(long lastModified);
    void setCreated(long created);

    long getAddress();

    void flush();
}
