package dev.ultreon.devicesnext.mineos;

@FunctionalInterface
public interface ApplicationFactory<T extends Application> {
    T create();
}
