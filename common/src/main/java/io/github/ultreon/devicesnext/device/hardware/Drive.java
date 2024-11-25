package io.github.ultreon.devicesnext.device.hardware;

public abstract class Drive implements HardwareComponent {
    abstract FileHandle get(String path);

    abstract FileHandle createFile(String path);
}
