package com.ultreon.mods.lib.client.devicetest;

@FunctionalInterface
public interface ApplicationFactory<T extends Application> {
    T create();
}
