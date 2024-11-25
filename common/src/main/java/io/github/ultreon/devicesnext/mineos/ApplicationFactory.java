package io.github.ultreon.devicesnext.mineos;

@FunctionalInterface
public interface ApplicationFactory<T extends Application> {
    T create();
}
